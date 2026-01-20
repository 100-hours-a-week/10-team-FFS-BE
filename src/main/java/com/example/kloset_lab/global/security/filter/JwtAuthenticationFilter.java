package com.example.kloset_lab.global.security.filter;

import com.example.kloset_lab.auth.entity.TokenType;
import com.example.kloset_lab.global.security.jwt.JwtAuthenticationToken;
import com.example.kloset_lab.global.security.provider.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * JWT 인증 필터
 *
 * <p>모든 요청에서 Authorization 헤더의 Bearer 토큰을 검증하고,
 * 유효한 경우 SecurityContext에 인증 정보를 설정한다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = resolveToken(request);

        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
            Long userId = jwtTokenProvider.getUserIdFromToken(token);
            TokenType tokenType = jwtTokenProvider.getTokenTypeFromToken(token);

            JwtAuthenticationToken authentication =
                    new JwtAuthenticationToken(userId, tokenType, List.of(new SimpleGrantedAuthority("ROLE_USER")));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("인증 성공 - userId: {}, tokenType: {}", userId, tokenType);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Authorization 헤더에서 Bearer 토큰 추출
     *
     * @param request HTTP 요청
     * @return 토큰 문자열 (없으면 null)
     */
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }

        return null;
    }
}
