package com.example.kloset_lab.chat.config;

import com.example.kloset_lab.auth.entity.TokenType;
import com.example.kloset_lab.chat.constant.ChatConstants;
import com.example.kloset_lab.global.security.provider.JwtTokenProvider;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * STOMP CONNECT 프레임에서 JWT 인증을 처리하는 인터셉터
 *
 * <p>인증 성공 시 simpSessionAttributes에 userId를 저장하여 이후 메시지 핸들러에서 활용한다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = resolveToken(accessor);

            if (!StringUtils.hasText(token) || !jwtTokenProvider.validateToken(token)) {
                log.warn("STOMP CONNECT 인증 실패: 유효하지 않은 토큰");
                throw new MessageDeliveryException("인증이 필요합니다.");
            }

            TokenType tokenType;
            try {
                tokenType = jwtTokenProvider.getTokenTypeFromToken(token);
            } catch (IllegalArgumentException e) {
                log.warn("STOMP CONNECT 인증 실패: type claim 없음 또는 알 수 없는 토큰 타입");
                throw new MessageDeliveryException("인증이 필요합니다.");
            }
            if (tokenType != TokenType.ACTIVE) {
                log.warn("STOMP CONNECT 인증 실패: ACTIVE 토큰이 아님 (type: {})", tokenType);
                throw new MessageDeliveryException("가입이 완료된 계정만 채팅을 이용할 수 있습니다.");
            }

            Long userId = jwtTokenProvider.getUserIdFromToken(token);
            accessor.setUser(() -> String.valueOf(userId));
            accessor.getSessionAttributes().put(ChatConstants.SESSION_ATTR_USER_ID, userId);
            log.debug("STOMP 인증 성공 - userId: {}", userId);
        }

        return message;
    }

    /**
     * STOMP 헤더에서 Bearer 토큰 추출
     *
     * @param accessor STOMP 헤더 접근자
     * @return 토큰 문자열 (없으면 null)
     */
    private String resolveToken(StompHeaderAccessor accessor) {
        return Optional.ofNullable(accessor.getFirstNativeHeader(AUTHORIZATION_HEADER))
                .filter(header -> header.startsWith(BEARER_PREFIX))
                .map(header -> header.substring(BEARER_PREFIX.length()))
                .orElse(null);
    }
}
