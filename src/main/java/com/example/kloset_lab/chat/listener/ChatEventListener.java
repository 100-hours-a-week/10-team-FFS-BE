package com.example.kloset_lab.chat.listener;

import com.example.kloset_lab.chat.constant.ChatConstants;
import com.example.kloset_lab.chat.dto.LastMessageDto;
import com.example.kloset_lab.chat.entity.ChatParticipant;
import com.example.kloset_lab.chat.event.ChatMessageSentEvent;
import com.example.kloset_lab.chat.event.ChatParticipantLeftEvent;
import com.example.kloset_lab.chat.event.ChatReadEvent;
import com.example.kloset_lab.chat.event.ChatRoomCreatedEvent;
import com.example.kloset_lab.chat.event.ChatRoomDeletedEvent;
import com.example.kloset_lab.chat.infrastructure.ChatEventPublisher;
import com.example.kloset_lab.chat.infrastructure.ChatRedisRepository;
import com.example.kloset_lab.chat.repository.ChatMessageRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 채팅 도메인 이벤트 리스너
 *
 * <p>MySQL 트랜잭션 커밋 이후에만 Redis·MongoDB 부수 작업을 실행하여 멀티 DB 정합성을 보장한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatEventListener {

    private final ChatRedisRepository chatRedisRepository;
    private final ChatEventPublisher chatEventPublisher;
    private final ChatMessageRepository chatMessageRepository;

    /**
     * 메시지 전송 후 Redis 캐시 갱신 및 Pub/Sub 브로드캐스트
     *
     * @param event 메시지 전송 이벤트
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessageSent(ChatMessageSentEvent event) {
        try {
            double score = (double) event.sentAt().toEpochMilli();
            Map<String, String> lastMsgFields = buildLastMsgFields(event);

            for (ChatParticipant participant : event.participants()) {
                chatRedisRepository.addRoomToUser(participant.getUserId(), event.roomId(), score);
                if (!participant.getUserId().equals(event.senderId())) {
                    chatRedisRepository.incrementUnread(participant.getUserId(), event.roomId());
                }
            }
            chatRedisRepository.setLastMessage(event.roomId(), lastMsgFields);

            chatEventPublisher.publishRoomMessage(event.roomId(), event.broadcastMessage());
            event.participants()
                    .forEach(p -> chatEventPublisher.publishRoomUpdate(
                            p.getUserId(),
                            com.example.kloset_lab.chat.dto.stomp.ChatRoomUpdateEvent.builder()
                                    .roomId(event.roomId())
                                    .lastMessage(LastMessageDto.builder()
                                            .messageId(event.messageId())
                                            .content(event.contentPreview())
                                            .type(event.type())
                                            .sentAt(event.sentAt())
                                            .build())
                                    .senderId(event.senderId())
                                    .build()));
        } catch (Exception e) {
            log.error(
                    "메시지 전송 후 Redis/Pub-Sub 처리 실패 - roomId: {}, messageId: {}. 보정 스케줄러가 복구합니다.",
                    event.roomId(),
                    event.messageId(),
                    e);
        }
    }

    /**
     * 채팅방 생성 후 Redis 목록 캐시 등록
     *
     * @param event 채팅방 생성 이벤트
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRoomCreated(ChatRoomCreatedEvent event) {
        try {
            chatRedisRepository.addRoomToUser(event.userId(), event.roomId(), event.score());
            chatRedisRepository.addRoomToUser(event.opponentUserId(), event.roomId(), event.score());
        } catch (Exception e) {
            log.error("채팅방 생성 후 Redis 캐시 등록 실패 - roomId: {}. rebuildRoomCache로 복구됩니다.", event.roomId(), e);
        }
    }

    /**
     * 채팅방 퇴장 후 Redis 캐시 정리
     *
     * @param event 참여자 퇴장 이벤트
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onParticipantLeft(ChatParticipantLeftEvent event) {
        try {
            chatRedisRepository.removeRoomFromUser(event.userId(), event.roomId());
            chatRedisRepository.deleteUnread(event.userId(), event.roomId());
        } catch (Exception e) {
            log.error(
                    "채팅방 퇴장 후 Redis 캐시 정리 실패 - userId: {}, roomId: {}. TTL 만료 후 자동 정리됩니다.",
                    event.userId(),
                    event.roomId(),
                    e);
        }
    }

    /**
     * 채팅방 삭제 후 Redis·MongoDB 정리
     *
     * @param event 채팅방 삭제 이벤트
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRoomDeleted(ChatRoomDeletedEvent event) {
        try {
            chatRedisRepository.deleteLastMessage(event.roomId());
            chatMessageRepository.deleteAllByRoomId(event.roomId());
        } catch (Exception e) {
            log.error("채팅방 삭제 후 Redis·MongoDB 정리 실패 - roomId: {}. 보정 스케줄러가 복구합니다.", event.roomId(), e);
        }
    }

    /**
     * 읽음 처리 후 Redis 미읽음 카운터 초기화
     *
     * @param event 읽음 처리 이벤트
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRead(ChatReadEvent event) {
        try {
            chatRedisRepository.resetUnread(event.userId(), event.roomId());
        } catch (Exception e) {
            log.error(
                    "읽음 처리 후 Redis 미읽음 초기화 실패 - userId: {}, roomId: {}. 다음 읽음 처리 시 보정됩니다.",
                    event.userId(),
                    event.roomId(),
                    e);
        }
    }

    private Map<String, String> buildLastMsgFields(ChatMessageSentEvent event) {
        Map<String, String> fields = new HashMap<>();
        fields.put(ChatConstants.FIELD_LAST_MESSAGE_ID, event.messageId());
        fields.put(
                ChatConstants.FIELD_LAST_MESSAGE_CONTENT,
                Optional.ofNullable(event.contentPreview()).orElse(""));
        fields.put(
                ChatConstants.FIELD_LAST_MESSAGE_TYPE,
                Optional.ofNullable(event.type()).orElse(""));
        fields.put(
                ChatConstants.FIELD_LAST_MESSAGE_AT,
                String.valueOf(event.sentAt().toEpochMilli()));
        return fields;
    }
}
