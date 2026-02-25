package com.example.kloset_lab.chat.scheduler;

import com.example.kloset_lab.chat.constant.ChatConstants;
import com.example.kloset_lab.chat.entity.ChatRoom;
import com.example.kloset_lab.chat.infrastructure.ChatRedisRepository;
import com.example.kloset_lab.chat.repository.ChatMessageRepository;
import com.example.kloset_lab.chat.repository.ChatRoomRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 멀티 DB 정합성 보정 스케줄러
 *
 * <p>@TransactionalEventListener 이벤트가 앱 크래시로 소실된 경우를 대비하여 매일 새벽 3시에 불일치를 탐지하고 복구한다.</p>
 *
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatOrphanCleanupScheduler {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRedisRepository chatRedisRepository;

    /**
     * <p>leaveRoom AFTER_COMMIT 이벤트 소실 시 MongoDB에 메시지가 잔류할 수 있으므로, soft delete 상태인 방을 순회하여 정리한다.
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupOrphanMessages() {
        List<ChatRoom> deletedRooms = chatRoomRepository.findAllByDeletedAtIsNotNull();
        if (deletedRooms.isEmpty()) {
            return;
        }

        int cleaned = 0;
        for (ChatRoom room : deletedRooms) {
            long count = chatMessageRepository.countByRoomId(room.getId());
            if (count > 0) {
                chatMessageRepository.deleteAllByRoomId(room.getId());
                log.info("고아 메시지 정리 완료 - roomId: {}, 삭제된 메시지 수: {}", room.getId(), count);
                cleaned++;
            }
        }

        if (cleaned > 0) {
            log.info("고아 메시지 정리 완료 — {}개 채팅방 MongoDB 메시지 삭제", cleaned);
        }
    }

    /**
     * MySQL 스냅샷과 MongoDB 실제 마지막 메시지 재동기화
     *
     * <p>sendMessage AFTER_COMMIT 이벤트 소실 시 MySQL snapshot에 lastMessageId가 있으나 MongoDB에 해당 메시지가 없는 경우, MongoDB 기준으로 MySQL을 재동기화한다.
     */
    @Scheduled(cron = "0 10 3 * * *")
    @Transactional
    public void reconcileLastMessageSnapshot() {
        List<ChatRoom> activeRooms = chatRoomRepository.findAllByDeletedAtIsNull();

        int reconciled = 0;
        for (ChatRoom room : activeRooms) {
            String snapshotMessageId = room.getLastMessageId();
            if (snapshotMessageId == null || !ObjectId.isValid(snapshotMessageId)) {
                continue;
            }

            boolean existsInMongo = chatMessageRepository.existsById(new ObjectId(snapshotMessageId));
            if (existsInMongo) {
                continue;
            }

            // MySQL 스냅샷에는 있으나 MongoDB에 없음 → MongoDB 실제 마지막 메시지로 재동기화
            chatMessageRepository
                    .findByRoomId(room.getId(), PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "_id")))
                    .stream()
                    .findFirst()
                    .ifPresentOrElse(
                            actual -> {
                                String preview = ChatConstants.toPreview(actual.getType(), actual.getContent());
                                room.updateLastMessage(
                                        actual.getId().toHexString(), preview, actual.getType(), actual.getCreatedAt());

                                Map<String, String> fields = new HashMap<>();
                                fields.put(
                                        ChatConstants.FIELD_LAST_MESSAGE_ID,
                                        actual.getId().toHexString());
                                fields.put(ChatConstants.FIELD_LAST_MESSAGE_CONTENT, preview);
                                fields.put(
                                        ChatConstants.FIELD_LAST_MESSAGE_TYPE,
                                        Optional.ofNullable(actual.getType()).orElse(""));
                                fields.put(
                                        ChatConstants.FIELD_LAST_MESSAGE_AT,
                                        String.valueOf(actual.getCreatedAt().toEpochMilli()));
                                chatRedisRepository.setLastMessage(room.getId(), fields);

                                log.info(
                                        "MySQL 스냅샷 재동기화 완료 - roomId: {}, 잘못된 snapshotId: {}, 실제 lastMessageId: {}",
                                        room.getId(),
                                        snapshotMessageId,
                                        actual.getId().toHexString());
                            },
                            () -> {
                                // MongoDB에 메시지가 아예 없으면 스냅샷 초기화
                                room.clearLastMessage();
                                log.warn("roomId: {}의 MongoDB 메시지가 전혀 없어 스냅샷 초기화", room.getId());
                            });
            reconciled++;
        }

        if (reconciled > 0) {
            log.info("MySQL 스냅샷 재동기화 완료 — {}개 채팅방 처리", reconciled);
        }
    }
}
