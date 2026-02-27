package com.example.kloset_lab.global.config;

import com.example.kloset_lab.chat.config.StompAuthChannelInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;

    // @EnableWebSocketMessageBroker 인프라가 생성하는 스케줄러 재사용
    // - @Lazy: configureMessageBroker 호출 시점에 해당 빈이 아직 초기화 중일 수 있으므로 지연 주입
    @Lazy
    @Autowired
    private TaskScheduler messageBrokerTaskScheduler;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 클라이언트 → 서버 메시지 prefix
        registry.setApplicationDestinationPrefixes("/app");
        // 브로커 destination prefix: /topic (브로드캐스트), /queue (user-specific queue 내부 변환 경로)
        registry.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(new long[] {30000, 30000})
                .setTaskScheduler(messageBrokerTaskScheduler);
        // 특정 사용자 목적지 prefix
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthChannelInterceptor);
    }
}
