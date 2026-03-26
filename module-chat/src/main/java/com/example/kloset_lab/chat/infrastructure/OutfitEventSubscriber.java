package com.example.kloset_lab.chat.infrastructure;

import com.example.kloset_lab.global.infrastructure.OutfitWebSocketMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * 코디추천 Redis Pub/Sub 구독자 → WebSocket push
 *
 * <p>module-api가 발행한 outfit 이벤트를 수신하여 STOMP 클라이언트로 전달한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutfitEventSubscriber {

    private static final String OUTFIT_CHANNEL_PREFIX = "outfit:event:";

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 코디추천 이벤트 수신 → /user/{userId}/queue/outfit-events 전달
     *
     * @param message JSON 직렬화된 OutfitWebSocketMessage
     * @param channel Redis 채널명 (outfit:event:{userId})
     */
    public void onOutfitEvent(String message, String channel) {
        try {
            OutfitWebSocketMessage payload = objectMapper.readValue(message, OutfitWebSocketMessage.class);
            String userId = channel.substring(OUTFIT_CHANNEL_PREFIX.length());
            messagingTemplate.convertAndSendToUser(userId, "/queue/outfit-events", payload);
            log.debug(
                    "[OutfitEventSubscriber] WebSocket push - userId: {}, requestId: {}, status: {}",
                    userId,
                    payload.requestId(),
                    payload.status());
        } catch (JsonProcessingException e) {
            log.error("[OutfitEventSubscriber] 역직렬화 실패 - channel: {}, error: {}", channel, e.getMessage());
        }
    }
}
