package com.example.kloset_lab.global.security.jwt;

import com.example.kloset_lab.auth.entity.TokenType;
import com.example.kloset_lab.global.security.filter.JwtAuthenticationFilter;
import java.util.Collection;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

/**
 * JWT 기반 인증 토큰 (Spring Security Authentication 구현체)
 *
 * <p>왜 커스텀 클래스를 만들었는가?</p>
 * <ul>
 *   <li>기본 제공되는 UsernamePasswordAuthenticationToken은 TokenType을 담을 명시적인 필드가 없음</li>
 *   <li>authorities나 details에 문자열로 저장하면 런타임 파싱/캐스팅이 필요하고 타입 안전성이 떨어짐</li>
 *   <li>TokenType을 명시적 필드로 두면 컴파일 타임에 타입 체크가 가능하고, 코드 가독성이 향상됨</li>
 * </ul>
 *
 * <p>사용 예시:</p>
 * <pre>{@code
 * // JwtAuthenticationFilter에서 인증 객체 생성
 * JwtAuthenticationToken auth = new JwtAuthenticationToken(userId, tokenType, authorities);
 * SecurityContextHolder.getContext().setAuthentication(auth);
 *
 * // Controller에서 TokenType 확인
 * JwtAuthenticationToken auth = (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
 * if (auth.getTokenType() == TokenType.ACTIVE) {
 *     // 기존 회원 전용 로직
 * }
 * }</pre>
 *
 * @see TokenType
 * @see JwtAuthenticationFilter
 */
public class JwtAuthenticationToken extends AbstractAuthenticationToken {

    private final Long userId;
    private final TokenType tokenType;

    /**
     * @param userId      인증된 회원의 ID (PK)
     * @param tokenType   토큰 타입 (ACTIVE: 기존 회원, REGISTRATION: 신규 회원)
     * @param authorities 권한 목록 (ROLE_USER 등)
     */
    public JwtAuthenticationToken(
            Long userId, TokenType tokenType, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.userId = userId;
        this.tokenType = tokenType;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    /**
     * @return 회원 ID (Principal로 사용)
     */
    @Override
    public Object getPrincipal() {
        return userId;
    }

    public Long getUserId() {
        return userId;
    }

    public TokenType getTokenType() {
        return tokenType;
    }
}
