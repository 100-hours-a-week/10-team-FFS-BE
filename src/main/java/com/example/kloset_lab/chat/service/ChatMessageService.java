package com.example.kloset_lab.chat.service;

import com.example.kloset_lab.chat.config.CdnProperties;
import com.example.kloset_lab.chat.constant.ChatConstants;
import com.example.kloset_lab.chat.document.ChatImage;
import com.example.kloset_lab.chat.document.ChatMessage;
import com.example.kloset_lab.chat.dto.ChatImageDto;
import com.example.kloset_lab.chat.dto.stomp.ChatBroadcastMessage;
import com.example.kloset_lab.chat.dto.stomp.ChatSendRequest;
import com.example.kloset_lab.chat.entity.ChatParticipant;
import com.example.kloset_lab.chat.entity.ChatRoom;
import com.example.kloset_lab.chat.entity.RoomType;
import com.example.kloset_lab.chat.event.ChatMessageSentEvent;
import com.example.kloset_lab.chat.repository.ChatMessageRepository;
import com.example.kloset_lab.chat.repository.ChatParticipantRepository;
import com.example.kloset_lab.chat.repository.ChatRoomRepository;
import com.example.kloset_lab.feed.repository.FeedRepository;
import com.example.kloset_lab.global.exception.CustomException;
import com.example.kloset_lab.global.exception.ErrorCode;
import com.example.kloset_lab.media.entity.MediaFile;
import com.example.kloset_lab.media.entity.Purpose;
import com.example.kloset_lab.media.repository.MediaFileRepository;
import com.example.kloset_lab.media.service.MediaService;
import com.example.kloset_lab.user.entity.UserProfile;
import com.example.kloset_lab.user.repository.UserProfileRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatParticipantRepository chatParticipantRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final MediaFileRepository mediaFileRepository;
    private final MediaService mediaService;
    private final FeedRepository feedRepository;
    private final UserProfileRepository userProfileRepository;
    private final CdnProperties cdnProperties;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 채팅 메시지 전송 처리
     *
     * <p>MongoDB 저장(메시지 무결성 우선) → MySQL 스냅샷 갱신(@Transactional) → AFTER_COMMIT 이벤트로 Redis·Pub/Sub 처리
     *
     * @param userId 발신자 ID
     * @param roomId 채팅방 ID
     * @param req    메시지 전송 요청
     */
    @Transactional
    public void sendMessage(Long userId, Long roomId, ChatSendRequest req) {
        // 0. 메시지 타입 유효성 검증
        validateMessageType(req);

        // 1. 참여자·방·미디어 파일 검증 (MySQL 읽기)
        chatParticipantRepository
                .findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_ACCESS_DENIED));

        ChatRoom room = chatRoomRepository
                .findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        List<ChatImage> chatImages = validateAndBuildImages(userId, req);

        if (ChatConstants.MSG_TYPE_FEED.equals(req.type())) {
            feedRepository
                    .findById(req.relatedFeedId())
                    .orElseThrow(() -> new CustomException(ErrorCode.FEED_NOT_FOUND));
        }

        Instant now = Instant.now();

        // 2. MongoDB 메시지 저장 (메시지 무결성 최우선 — @Transactional 범위 밖)
        ChatMessage savedMessage = chatMessageRepository.save(ChatMessage.builder()
                .roomId(roomId)
                .senderId(userId)
                .type(req.type())
                .content(req.content())
                .images(chatImages)
                .relatedFeedId(req.relatedFeedId())
                .createdAt(now)
                .build());

        String messageId = savedMessage.getId().toHexString();
        String contentPreview = buildContentPreview(req);

        // 3. MySQL chat_room 스냅샷 갱신 (@Transactional 범위 내)
        room.updateLastMessage(messageId, contentPreview, req.type(), now);

        // 4. 브로드캐스트 메시지 구성
        String senderNickname = userProfileRepository
                .findByUserId(userId)
                .map(UserProfile::getNickname)
                .orElse(null);

        ChatBroadcastMessage broadcastMessage = ChatBroadcastMessage.builder()
                .messageId(messageId)
                .roomId(roomId)
                .senderId(userId)
                .senderNickname(senderNickname)
                .type(req.type())
                .content(req.content())
                .images(buildImageDtos(chatImages))
                .relatedFeedId(req.relatedFeedId())
                .clientMessageId(req.clientMessageId())
                .createdAt(now)
                .build();

        // DM: 나간 참여자 자동 재진입 후 전체 브로드캐스트 (채팅목록 재등록 + unread 갱신)
        // GROUP: 활성 참여자만 브로드캐스트
        List<ChatParticipant> participants;
        if (room.getType() == RoomType.DM) {
            participants = chatParticipantRepository.findByRoomId(roomId);
            participants.stream().filter(p -> p.getLeftAt() != null).forEach(ChatParticipant::reenter);
        } else {
            participants = chatParticipantRepository.findActiveByRoomId(roomId);
        }

        // 5. MySQL 커밋 이후 Redis 캐시·Pub/Sub 처리 (AFTER_COMMIT 이벤트)
        eventPublisher.publishEvent(new ChatMessageSentEvent(
                roomId, userId, participants, messageId, contentPreview, req.type(), now, broadcastMessage));
    }

    /**
     * 메시지 타입 및 타입별 필수 필드 검증
     *
     * @param req 메시지 전송 요청
     */
    private void validateMessageType(ChatSendRequest req) {
        String type = Optional.ofNullable(req.type()).orElseThrow(() -> new CustomException(ErrorCode.INVALID_REQUEST));
        switch (type) {
            case ChatConstants.MSG_TYPE_TEXT -> {
                String content = Optional.ofNullable(req.content())
                        .filter(c -> !c.isBlank())
                        .orElseThrow(() -> new CustomException(ErrorCode.INVALID_REQUEST));
                if (content.length() > 255) {
                    throw new CustomException(ErrorCode.INVALID_REQUEST);
                }
            }
            case ChatConstants.MSG_TYPE_IMAGE -> Optional.ofNullable(req.mediaFileIds())
                    .filter(ids -> !ids.isEmpty())
                    .orElseThrow(() -> new CustomException(ErrorCode.INVALID_REQUEST));
            case ChatConstants.MSG_TYPE_FEED -> Optional.ofNullable(req.relatedFeedId())
                    .orElseThrow(() -> new CustomException(ErrorCode.INVALID_REQUEST));
            default -> throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
    }

    /**
     * 이미지 타입 메시지의 media_file 검증(PENDING→UPLOADED 전환) 및 ChatImage 목록 생성
     *
     * <p>소유자·purpose·S3 존재 확인 후 파일 상태를 PENDING→UPLOADED로 전환한다.
     * MongoDB 저장 실패 시 @Transactional 롤백으로 상태가 PENDING으로 자동 복원된다.
     */
    private List<ChatImage> validateAndBuildImages(Long userId, ChatSendRequest req) {
        if (!ChatConstants.MSG_TYPE_IMAGE.equals(req.type())
                || req.mediaFileIds() == null
                || req.mediaFileIds().isEmpty()) {
            return List.of();
        }

        // 소유자·purpose·S3 존재 확인 후 PENDING → UPLOADED 전환
        // (TOO_MANY_FILES, FILE_NOT_FOUND, FILE_ACCESS_DENIED, UPLOADED_FILE_MISMATCH, NOT_PENDING_STATE 예외 포함)
        mediaService.confirmFileUpload(userId, Purpose.CHAT, req.mediaFileIds());

        Map<Long, MediaFile> fileMap = mediaFileRepository.findAllById(req.mediaFileIds()).stream()
                .collect(Collectors.toMap(MediaFile::getId, f -> f));

        List<ChatImage> chatImages = new ArrayList<>();
        for (int i = 0; i < req.mediaFileIds().size(); i++) {
            Long mediaFileId = req.mediaFileIds().get(i);
            chatImages.add(ChatImage.builder()
                    .mediaFileId(mediaFileId)
                    .objectKey(fileMap.get(mediaFileId).getObjectKey())
                    .displayOrder(i + 1)
                    .build());
        }
        return chatImages;
    }

    private String buildContentPreview(ChatSendRequest req) {
        return ChatConstants.toPreview(req.type(), req.content());
    }

    private List<ChatImageDto> buildImageDtos(List<ChatImage> chatImages) {
        if (chatImages == null || chatImages.isEmpty()) {
            return List.of();
        }
        return chatImages.stream()
                .map(img -> ChatImageDto.builder()
                        .mediaFileId(img.getMediaFileId())
                        .imageUrl(cdnProperties.getBaseUrl() + "/" + img.getObjectKey())
                        .displayOrder(img.getDisplayOrder())
                        .build())
                .collect(Collectors.toList());
    }
}
