package com.example.kloset_lab.chat.infrastructure;

import com.example.kloset_lab.chat.dto.stomp.ChatBroadcastMessage;
import com.example.kloset_lab.chat.dto.stomp.ChatRoomUpdateEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis Pub/Sub 이벤트 구독자
 *
 * <p>모든 서버 인스턴스에서 동작하며, Redis에서 수신한 메시지를 STOMP 클라이언트로 브로드캐스트한다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatEventSubscriber {

    private static final String ROOM_MSG_PREFIX = "chat:msg:";
    private static final String ROOM_UPDATE_PREFIX = "chat:update:";

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 채팅방 메시지 수신 → STOMP /topic/room/{roomId} 브로드캐스트
     *
     * @param message 직렬화된 JSON 메시지
     * @param channel Redis 채널명 (chat:msg:{roomId})
     */
    public void onRoomMessage(String message, String channel) {
        try {
            ChatBroadcastMessage payload = objectMapper.readValue(message, ChatBroadcastMessage.class);
            String roomId = channel.substring(ROOM_MSG_PREFIX.length());
            messagingTemplate.convertAndSend("/topic/room/" + roomId, payload);
            log.debug("채팅 메시지 브로드캐스트 - roomId: {}, messageId: {}", roomId, payload.messageId());
        } catch (JsonProcessingException e) {
            log.error("채팅 메시지 역직렬화 실패 - channel: {}, error: {}", channel, e.getMessage());
        }
    }

    /**
     * 채팅 목록 갱신 이벤트 수신 → STOMP /user/{userId}/queue/chat-room-updates 전달
     *
     * @param message 직렬화된 JSON 이벤트
     * @param channel Redis 채널명 (chat:update:{userId})
     */
    public void onRoomUpdate(String message, String channel) {
        try {
            ChatRoomUpdateEvent payload = objectMapper.readValue(message, ChatRoomUpdateEvent.class);
            String userId = channel.substring(ROOM_UPDATE_PREFIX.length());
            messagingTemplate.convertAndSendToUser(userId, "/queue/chat-room-updates", payload);
            log.debug("채팅 목록 갱신 이벤트 전달 - userId: {}, roomId: {}", userId, payload.roomId());
        } catch (JsonProcessingException e) {
            log.error("채팅 목록 갱신 이벤트 역직렬화 실패 - channel: {}, error: {}", channel, e.getMessage());
        }
    }
}
