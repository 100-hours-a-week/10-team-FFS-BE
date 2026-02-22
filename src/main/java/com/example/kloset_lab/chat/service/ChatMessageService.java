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
import com.example.kloset_lab.chat.event.ChatMessageSentEvent;
import com.example.kloset_lab.chat.repository.ChatMessageRepository;
import com.example.kloset_lab.chat.repository.ChatParticipantRepository;
import com.example.kloset_lab.chat.repository.ChatRoomRepository;
import com.example.kloset_lab.feed.repository.FeedRepository;
import com.example.kloset_lab.global.exception.CustomException;
import com.example.kloset_lab.global.exception.ErrorCode;
import com.example.kloset_lab.media.entity.FileStatus;
import com.example.kloset_lab.media.entity.MediaFile;
import com.example.kloset_lab.media.entity.Purpose;
import com.example.kloset_lab.media.repository.MediaFileRepository;
import com.example.kloset_lab.user.entity.UserProfile;
import com.example.kloset_lab.user.repository.UserProfileRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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
        // 1. 참여자·방·미디어 파일 검증 (MySQL 읽기)
        chatParticipantRepository
                .findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_ACCESS_DENIED));

        ChatRoom room = chatRoomRepository
                .findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        List<ChatImage> chatImages = validateAndBuildImages(userId, req);

        if (ChatConstants.MSG_TYPE_FEED.equals(req.type()) && req.relatedFeedId() != null) {
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

        List<ChatParticipant> participants = chatParticipantRepository.findByRoomId(roomId);

        // 5. MySQL 커밋 이후 Redis 캐시·Pub/Sub 처리 (AFTER_COMMIT 이벤트)
        eventPublisher.publishEvent(new ChatMessageSentEvent(
                roomId, userId, participants, messageId, contentPreview, req.type(), now, broadcastMessage));
    }

    /** 이미지 타입 메시지의 media_file 검증 및 ChatImage 목록 생성 */
    private List<ChatImage> validateAndBuildImages(Long userId, ChatSendRequest req) {
        if (!ChatConstants.MSG_TYPE_IMAGE.equals(req.type())
                || req.mediaFileIds() == null
                || req.mediaFileIds().isEmpty()) {
            return null;
        }

        if (req.mediaFileIds().size() > Purpose.CHAT.getMaxCount()) {
            throw new CustomException(ErrorCode.TOO_MANY_FILES);
        }

        List<ChatImage> chatImages = new ArrayList<>();
        for (int i = 0; i < req.mediaFileIds().size(); i++) {
            Long mediaFileId = req.mediaFileIds().get(i);
            MediaFile mediaFile = mediaFileRepository
                    .findById(mediaFileId)
                    .orElseThrow(() -> new CustomException(ErrorCode.FILE_NOT_FOUND));

            if (!mediaFile.getUser().getId().equals(userId)) {
                throw new CustomException(ErrorCode.FILE_ACCESS_DENIED);
            }
            if (mediaFile.getPurpose() != Purpose.CHAT) {
                throw new CustomException(ErrorCode.INVALID_REQUEST);
            }
            if (mediaFile.getStatus() != FileStatus.UPLOADED) {
                throw new CustomException(ErrorCode.INVALID_REQUEST);
            }

            chatImages.add(ChatImage.builder()
                    .mediaFileId(mediaFileId)
                    .objectKey(mediaFile.getObjectKey())
                    .displayOrder(i)
                    .build());
        }
        return chatImages;
    }

    private String buildContentPreview(ChatSendRequest req) {
        return switch (req.type()) {
            case ChatConstants.MSG_TYPE_TEXT -> Optional.ofNullable(req.content())
                    .map(c -> c.length() > ChatConstants.CONTENT_PREVIEW_MAX_LENGTH
                            ? c.substring(0, ChatConstants.CONTENT_PREVIEW_MAX_LENGTH)
                            : c)
                    .orElse("");
            case ChatConstants.MSG_TYPE_IMAGE -> ChatConstants.PREVIEW_IMAGE;
            case ChatConstants.MSG_TYPE_FEED -> ChatConstants.PREVIEW_FEED;
            default -> "";
        };
    }

    private List<ChatImageDto> buildImageDtos(List<ChatImage> chatImages) {
        return Optional.ofNullable(chatImages)
                .map(images -> images.stream()
                        .map(img -> ChatImageDto.builder()
                                .mediaFileId(img.getMediaFileId())
                                .imageUrl(cdnProperties.getBaseUrl() + "/" + img.getObjectKey())
                                .displayOrder(img.getDisplayOrder())
                                .build())
                        .collect(Collectors.toList()))
                .orElse(null);
    }
}
