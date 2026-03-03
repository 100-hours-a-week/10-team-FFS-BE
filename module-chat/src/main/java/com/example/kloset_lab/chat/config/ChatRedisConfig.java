package com.example.kloset_lab.chat.config;

import com.example.kloset_lab.chat.infrastructure.ChatEventSubscriber;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * 채팅 전용 Redis Pub/Sub 설정
 *
 * <p>StringRedisTemplate은 Spring Boot 자동 구성에 위임한다.
 * 이 Config는 채팅 메시지 구독에 필요한 RedisMessageListenerContainer만 담당한다.</p>
 */
@Configuration
@RequiredArgsConstructor
public class ChatRedisConfig {

    private final ChatEventSubscriber chatEventSubscriber;

    /**
     * Redis Pub/Sub 메시지 리스너 컨테이너
     *
     * @param connectionFactory Redis 연결 팩토리
     * @return 메시지 리스너 컨테이너
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        // 채팅 메시지 브로드캐스트 구독 (채팅방 ID 패턴)
        container.addMessageListener(
                (message, pattern) -> chatEventSubscriber.onRoomMessage(
                        new String(message.getBody()), new String(message.getChannel())),
                new PatternTopic("chat:msg:*"));

        // 채팅 목록 갱신 이벤트 구독 (사용자 ID 패턴)
        container.addMessageListener(
                (message, pattern) -> chatEventSubscriber.onRoomUpdate(
                        new String(message.getBody()), new String(message.getChannel())),
                new PatternTopic("chat:update:*"));

        return container;
    }
}
