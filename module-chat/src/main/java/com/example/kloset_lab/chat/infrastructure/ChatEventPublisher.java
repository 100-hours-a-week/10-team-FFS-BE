package com.example.kloset_lab.chat.infrastructure;

import com.example.kloset_lab.chat.dto.stomp.ChatBroadcastMessage;
import com.example.kloset_lab.chat.dto.stomp.ChatRoomUpdateEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/** Redis Pub/Sub 이벤트 발행자 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatEventPublisher {

    private static final String ROOM_MSG_CHANNEL = "chat:msg:%d";
    private static final String ROOM_UPDATE_CHANNEL = "chat:update:%d";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 채팅방 메시지 브로드캐스트 발행
     *
     * @param roomId  채팅방 ID
     * @param message 브로드캐스트 메시지
     */
    public void publishRoomMessage(Long roomId, ChatBroadcastMessage message) {
        String channel = String.format(ROOM_MSG_CHANNEL, roomId);
        publish(channel, message);
    }

    /**
     * 채팅 목록 갱신 이벤트 발행
     *
     * @param userId 대상 사용자 ID
     * @param event  목록 갱신 이벤트
     */
    public void publishRoomUpdate(Long userId, ChatRoomUpdateEvent event) {
        String channel = String.format(ROOM_UPDATE_CHANNEL, userId);
        publish(channel, event);
    }

    private void publish(String channel, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            stringRedisTemplate.convertAndSend(channel, json);
        } catch (JsonProcessingException e) {
            log.error("Redis Pub/Sub 발행 실패 - channel: {}, error: {}", channel, e.getMessage());
        }
    }
}
