package com.example.kloset_lab.chat.service;

import com.example.kloset_lab.chat.config.CdnProperties;
import com.example.kloset_lab.chat.constant.ChatConstants;
import com.example.kloset_lab.chat.document.ChatMessage;
import com.example.kloset_lab.chat.dto.ChatImageDto;
import com.example.kloset_lab.chat.dto.ChatMessageItem;
import com.example.kloset_lab.chat.dto.ChatMessageListResponse;
import com.example.kloset_lab.chat.dto.ChatRoomCreateRequest;
import com.example.kloset_lab.chat.dto.ChatRoomItem;
import com.example.kloset_lab.chat.dto.ChatRoomListResponse;
import com.example.kloset_lab.chat.dto.ChatRoomResponse;
import com.example.kloset_lab.chat.dto.ChatRoomResult;
import com.example.kloset_lab.chat.dto.LastMessageDto;
import com.example.kloset_lab.chat.dto.OpponentDto;
import com.example.kloset_lab.chat.dto.ReadRequest;
import com.example.kloset_lab.chat.dto.UnreadStatusResponse;
import com.example.kloset_lab.chat.entity.ChatParticipant;
import com.example.kloset_lab.chat.entity.ChatRoom;
import com.example.kloset_lab.chat.event.ChatParticipantLeftEvent;
import com.example.kloset_lab.chat.event.ChatReadEvent;
import com.example.kloset_lab.chat.event.ChatRoomCreatedEvent;
import com.example.kloset_lab.chat.event.ChatRoomDeletedEvent;
import com.example.kloset_lab.chat.infrastructure.ChatRedisRepository;
import com.example.kloset_lab.chat.repository.ChatMessageRepository;
import com.example.kloset_lab.chat.repository.ChatParticipantRepository;
import com.example.kloset_lab.chat.repository.ChatRoomRepository;
import com.example.kloset_lab.global.exception.CustomException;
import com.example.kloset_lab.global.exception.ErrorCode;
import com.example.kloset_lab.user.entity.UserProfile;
import com.example.kloset_lab.user.repository.UserProfileRepository;
import com.example.kloset_lab.user.repository.UserRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRedisRepository chatRedisRepository; // rebuildRoomCache·getUnreadStatus에서 사용
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final CdnProperties cdnProperties;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 채팅방 생성 또는 기존 방 반환
     *
     * @param userId  현재 사용자 ID
     * @param request 채팅방 생성 요청
     * @return 채팅방 응답
     */
    @Transactional
    public ChatRoomResult createOrGetRoom(Long userId, ChatRoomCreateRequest request) {
        Long opponentUserId = request.opponentUserId();

        if (userId.equals(opponentUserId)) {
            throw new CustomException(ErrorCode.CANNOT_CHAT_WITH_SELF);
        }

        userRepository.findById(opponentUserId).orElseThrow(() -> new CustomException(ErrorCode.TARGET_USER_NOT_FOUND));

        return chatRoomRepository
                .findExistingRoomBetweenUsers(userId, opponentUserId)
                .map(existingRoom -> ChatRoomResult.existing(buildChatRoomResponse(existingRoom, opponentUserId)))
                .orElseGet(() -> ChatRoomResult.created(createNewRoom(userId, opponentUserId)));
    }

    private ChatRoomResponse createNewRoom(Long userId, Long opponentUserId) {
        ChatRoom room = ChatRoom.create();
        chatRoomRepository.save(room);

        chatParticipantRepository.save(
                ChatParticipant.builder().room(room).userId(userId).build());
        chatParticipantRepository.save(
                ChatParticipant.builder().room(room).userId(opponentUserId).build());

        double score = (double) Instant.now().toEpochMilli();
        // MySQL 커밋 이후 Redis 캐시 등록 (AFTER_COMMIT 이벤트)
        eventPublisher.publishEvent(new ChatRoomCreatedEvent(userId, opponentUserId, room.getId(), score));

        return buildChatRoomResponse(room, opponentUserId);
    }

    private ChatRoomResponse buildChatRoomResponse(ChatRoom room, Long opponentUserId) {
        return ChatRoomResponse.builder()
                .roomId(room.getId())
                .opponent(buildOpponentDto(opponentUserId))
                .createdAt(room.getCreatedAt())
                .build();
    }

    /**
     * 사용자의 채팅방 목록 조회 (커서 기반 페이지네이션)
     *
     * @param userId 현재 사용자 ID
     * @param cursor 이전 페이지 마지막 score (lastMessageAt 밀리초)
     * @param size   조회 개수
     * @return 채팅방 목록 응답
     */
    @Transactional(readOnly = true)
    public ChatRoomListResponse getRooms(Long userId, Double cursor, int size) {
        if (chatRedisRepository.getRoomCount(userId) == 0) {
            rebuildRoomCache(userId);
        }

        double maxScore = Optional.ofNullable(cursor).orElse(Double.MAX_VALUE);
        List<String> roomIdStrs = chatRedisRepository.getRoomsDesc(userId, maxScore, size + 1);

        boolean hasNextPage = roomIdStrs.size() > size;
        List<String> pageRoomIds = hasNextPage ? roomIdStrs.subList(0, size) : roomIdStrs;

        List<ChatRoomItem> items = new ArrayList<>();
        Map<Long, Double> roomScores = new HashMap<>();
        Map<Long, Long> roomToOpponent = new HashMap<>();
        List<Long> opponentUserIds = new ArrayList<>();

        for (String roomIdStr : pageRoomIds) {
            long roomId = Long.parseLong(roomIdStr);
            Optional.ofNullable(chatRedisRepository.getRoomScore(userId, roomId))
                    .ifPresent(score -> roomScores.put(roomId, score));

            chatParticipantRepository.findByRoomId(roomId).stream()
                    .filter(p -> !p.getUserId().equals(userId))
                    .findFirst()
                    .ifPresent(p -> {
                        roomToOpponent.put(roomId, p.getUserId());
                        opponentUserIds.add(p.getUserId());
                    });
        }

        Map<Long, UserProfile> profileMap = userProfileRepository.findByUserIdIn(opponentUserIds).stream()
                .collect(Collectors.toMap(up -> up.getUser().getId(), up -> up));

        for (String roomIdStr : pageRoomIds) {
            long roomId = Long.parseLong(roomIdStr);
            Optional.ofNullable(roomToOpponent.get(roomId)).ifPresent(opponentUserId -> {
                LastMessageDto lastMessageDto = buildLastMessageDto(chatRedisRepository.getLastMessage(roomId))
                        .orElse(null);
                long unread = chatRedisRepository.getUnread(userId, roomId);
                OpponentDto opponentDto = buildOpponentDtoFromProfile(opponentUserId, profileMap.get(opponentUserId));

                items.add(ChatRoomItem.builder()
                        .roomId(roomId)
                        .opponent(opponentDto)
                        .lastMessage(lastMessageDto)
                        .unreadCount(unread)
                        .build());
            });
        }

        Double nextCursor =
                hasNextPage && !pageRoomIds.isEmpty() ? roomScores.get(Long.parseLong(pageRoomIds.getLast())) : null;

        return ChatRoomListResponse.builder()
                .rooms(items)
                .hasNextPage(hasNextPage)
                .nextCursor(nextCursor)
                .build();
    }

    private void rebuildRoomCache(Long userId) {
        chatParticipantRepository.findByUserId(userId).stream()
                .filter(p -> !p.getRoom().isDeleted())
                .forEach(participant -> {
                    ChatRoom room = participant.getRoom();
                    double score = (double) Optional.ofNullable(room.getLastMessageAt())
                            .map(Instant::toEpochMilli)
                            .orElse(room.getCreatedAt().toEpochMilli());

                    chatRedisRepository.addRoomToUser(userId, room.getId(), score);

                    Optional.ofNullable(room.getLastMessageId()).ifPresent(msgId -> {
                        Map<String, String> fields = new HashMap<>();
                        fields.put(ChatConstants.FIELD_LAST_MESSAGE_ID, msgId);
                        fields.put(
                                ChatConstants.FIELD_LAST_MESSAGE_CONTENT,
                                Optional.ofNullable(room.getLastMessageContent())
                                        .orElse(""));
                        fields.put(
                                ChatConstants.FIELD_LAST_MESSAGE_TYPE,
                                Optional.ofNullable(room.getLastMessageType()).orElse(""));
                        fields.put(
                                ChatConstants.FIELD_LAST_MESSAGE_AT,
                                Optional.ofNullable(room.getLastMessageAt())
                                        .map(at -> String.valueOf(at.toEpochMilli()))
                                        .orElse(""));
                        chatRedisRepository.setLastMessage(room.getId(), fields);
                    });

                    // Redis 재구축 시 MongoDB 기준으로 안읽은 메시지 수 복구
                    long unreadCount = Optional.ofNullable(participant.getLastReadMessageId())
                            .filter(ObjectId::isValid)
                            .map(lastReadId -> chatMessageRepository.countByRoomIdAndIdGreaterThan(
                                    room.getId(), new ObjectId(lastReadId)))
                            .orElseGet(() -> chatMessageRepository.countByRoomId(room.getId()));
                    if (unreadCount > 0) {
                        chatRedisRepository.setUnread(userId, room.getId(), unreadCount);
                    }
                });
    }

    /**
     * 채팅 메시지 목록 조회 (커서 기반)
     *
     * @param userId 현재 사용자 ID
     * @param roomId 채팅방 ID
     * @param cursor 이전 페이지 마지막 메시지 ObjectId 문자열
     * @param size   조회 개수
     * @return 메시지 목록 응답
     */
    @Transactional
    public ChatMessageListResponse getMessages(Long userId, Long roomId, String cursor, int size) {
        ChatParticipant participant = chatParticipantRepository
                .findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_ACCESS_DENIED));

        int limit = size > 0 ? size : ChatConstants.DEFAULT_MESSAGE_PAGE_SIZE;
        PageRequest pageable = PageRequest.of(0, limit + 1, Sort.by(Sort.Direction.DESC, "_id"));

        List<ChatMessage> messages = Optional.ofNullable(cursor)
                .filter(ObjectId::isValid)
                .map(c -> chatMessageRepository.findByRoomIdAndIdLessThan(roomId, new ObjectId(c), pageable))
                .orElseGet(() -> chatMessageRepository.findByRoomId(roomId, pageable));

        boolean hasNextPage = messages.size() > limit;
        List<ChatMessage> pageMessages = hasNextPage ? messages.subList(0, limit) : messages;

        List<ChatMessageItem> items =
                pageMessages.stream().map(this::buildMessageItem).collect(Collectors.toList());

        String nextCursor = hasNextPage && !pageMessages.isEmpty()
                ? pageMessages.get(pageMessages.size() - 1).getId().toHexString()
                : null;

        if (!pageMessages.isEmpty()) {
            String latestMessageId = pageMessages.get(0).getId().toHexString();
            applyReadEffect(participant, userId, roomId, latestMessageId);
        }

        return ChatMessageListResponse.builder()
                .messages(items)
                .hasNextPage(hasNextPage)
                .nextCursor(nextCursor)
                .build();
    }

    private void applyReadEffect(ChatParticipant participant, Long userId, Long roomId, String latestMessageId) {
        if (shouldUpdateLastRead(participant.getLastReadMessageId(), latestMessageId)) {
            participant.updateLastReadMessageId(latestMessageId);
            // MySQL 커밋 이후 Redis 미읽음 초기화 (AFTER_COMMIT 이벤트)
            eventPublisher.publishEvent(new ChatReadEvent(userId, roomId));
        }
    }

    /**
     * 채팅방 나가기
     *
     * @param userId 현재 사용자 ID
     * @param roomId 채팅방 ID
     */
    @Transactional
    public void leaveRoom(Long userId, Long roomId) {
        chatParticipantRepository
                .findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_ACCESS_DENIED));

        chatParticipantRepository.deleteByRoomIdAndUserId(roomId, userId);

        // MySQL 커밋 이후 Redis 캐시 정리 (AFTER_COMMIT 이벤트)
        eventPublisher.publishEvent(new ChatParticipantLeftEvent(userId, roomId));

        if (chatParticipantRepository.countByRoomId(roomId) == 0) {
            ChatRoom room = chatRoomRepository
                    .findById(roomId)
                    .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND));
            room.softDelete();
            // MySQL 커밋 이후 Redis·MongoDB 정리 (AFTER_COMMIT 이벤트)
            eventPublisher.publishEvent(new ChatRoomDeletedEvent(roomId));
        }
    }

    /**
     * 메시지 읽음 처리
     *
     * @param userId  현재 사용자 ID
     * @param roomId  채팅방 ID
     * @param request 읽음 처리 요청
     */
    @Transactional
    public void markAsRead(Long userId, Long roomId, ReadRequest request) {
        ChatParticipant participant = chatParticipantRepository
                .findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_ACCESS_DENIED));

        if (shouldUpdateLastRead(participant.getLastReadMessageId(), request.lastReadMessageId())) {
            participant.updateLastReadMessageId(request.lastReadMessageId());
            // MySQL 커밋 이후 Redis 미읽음 초기화 (AFTER_COMMIT 이벤트)
            eventPublisher.publishEvent(new ChatReadEvent(userId, roomId));
        }
    }

    /**
     * 전체 안읽은 메시지 현황 조회
     *
     * @param userId 현재 사용자 ID
     * @return 안읽은 메시지 현황
     */
    @Transactional(readOnly = true)
    public UnreadStatusResponse getUnreadStatus(Long userId) {
        long totalUnread = chatParticipantRepository.findByUserId(userId).stream()
                .mapToLong(
                        p -> chatRedisRepository.getUnread(userId, p.getRoom().getId()))
                .sum();
        return UnreadStatusResponse.builder()
                .hasUnread(totalUnread > 0)
                .totalUnreadCount(totalUnread)
                .build();
    }

    // ======================== 내부 헬퍼 ========================

    /**
     * lastReadMessageId 역방향 갱신 방지 (ObjectId 대소 비교)
     */
    private boolean shouldUpdateLastRead(String currentLastRead, String newMessageId) {
        return Optional.ofNullable(currentLastRead)
                .filter(ObjectId::isValid)
                .filter(id -> ObjectId.isValid(newMessageId))
                .map(id -> new ObjectId(newMessageId).compareTo(new ObjectId(id)) > 0)
                .orElse(true);
    }

    private ChatMessageItem buildMessageItem(ChatMessage msg) {
        List<ChatImageDto> images = Optional.ofNullable(msg.getImages())
                .map(imgs -> imgs.stream()
                        .map(img -> ChatImageDto.builder()
                                .mediaFileId(img.getMediaFileId())
                                .imageUrl(buildImageUrl(img.getObjectKey()))
                                .displayOrder(img.getDisplayOrder())
                                .build())
                        .collect(Collectors.toList()))
                .orElse(null);

        return ChatMessageItem.builder()
                .messageId(msg.getId().toHexString())
                .senderId(msg.getSenderId())
                .type(msg.getType())
                .content(msg.getContent())
                .images(images)
                .relatedFeedId(msg.getRelatedFeedId())
                .createdAt(msg.getCreatedAt())
                .build();
    }

    private Optional<LastMessageDto> buildLastMessageDto(Map<String, String> lastMsg) {
        return Optional.ofNullable(lastMsg).filter(m -> !m.isEmpty()).map(m -> {
            Instant sentAt = Optional.ofNullable(m.get(ChatConstants.FIELD_LAST_MESSAGE_AT))
                    .filter(s -> !s.isEmpty())
                    .flatMap(s -> {
                        try {
                            return Optional.of(Instant.ofEpochMilli(Long.parseLong(s)));
                        } catch (NumberFormatException e) {
                            return Optional.empty();
                        }
                    })
                    .orElse(null);
            return LastMessageDto.builder()
                    .messageId(m.get(ChatConstants.FIELD_LAST_MESSAGE_ID))
                    .content(m.get(ChatConstants.FIELD_LAST_MESSAGE_CONTENT))
                    .type(m.get(ChatConstants.FIELD_LAST_MESSAGE_TYPE))
                    .sentAt(sentAt)
                    .build();
        });
    }

    private OpponentDto buildOpponentDto(Long opponentUserId) {
        return userProfileRepository
                .findByUserId(opponentUserId)
                .map(profile -> buildOpponentDtoFromProfile(opponentUserId, profile))
                .orElseGet(() -> OpponentDto.builder().userId(opponentUserId).build());
    }

    private OpponentDto buildOpponentDtoFromProfile(Long opponentUserId, UserProfile profile) {
        return Optional.ofNullable(profile)
                .map(p -> {
                    String profileImageUrl = Optional.ofNullable(p.getProfileFile())
                            .map(file -> buildImageUrl(file.getObjectKey()))
                            .orElse(null);
                    return OpponentDto.builder()
                            .userId(opponentUserId)
                            .nickname(p.getNickname())
                            .profileImageUrl(profileImageUrl)
                            .build();
                })
                .orElseGet(() -> OpponentDto.builder().userId(opponentUserId).build());
    }

    private String buildImageUrl(String objectKey) {
        return Optional.ofNullable(objectKey)
                .map(key -> cdnProperties.getBaseUrl() + "/" + key)
                .orElse(null);
    }
}
