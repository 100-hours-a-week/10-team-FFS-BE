package com.example.kloset_lab.global.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis Pub/Sub 범용 이벤트 발행자
 *
 * <p>module-api, module-chat 등 여러 모듈에서 공통으로 사용한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisEventPublisher {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Redis 채널에 이벤트를 JSON 직렬화하여 발행한다.
     *
     * @param channel Redis 채널명
     * @param payload 발행할 객체 (JSON 직렬화 가능해야 함)
     */
    public void publish(String channel, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            stringRedisTemplate.convertAndSend(channel, json);
            log.debug("[RedisEventPublisher] 이벤트 발행 - channel: {}", channel);
        } catch (JsonProcessingException e) {
            log.error("[RedisEventPublisher] 직렬화 실패 - channel: {}, error: {}", channel, e.getMessage());
        }
    }
}
