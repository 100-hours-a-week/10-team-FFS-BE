package com.example.kloset_lab.global.security.provider;

import com.example.kloset_lab.auth.entity.TokenType;
import com.example.kloset_lab.global.security.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JwtTokenProvider {

    private static final String CLAIM_TOKEN_TYPE = "type";

    private final SecretKey secretKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes());
        this.accessTokenExpiration = jwtProperties.getAccessTokenExpiration();
        this.refreshTokenExpiration = jwtProperties.getRefreshTokenExpiration();
    }

    /**
     * Access Token 생성
     * @param userId 회원 ID (PK)
     * @param tokenType 토큰 타입 (ACTIVE / REGISTRATION)
     * @return JWT Access Token 문자열
     */
    public String generateAccessToken(Long userId, TokenType tokenType) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenExpiration);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_TOKEN_TYPE, tokenType.name())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    /**
     * Refresh Token 생성
     * @param userId 회원 ID (PK)
     * @return JWT Refresh Token 문자열
     */
    public String generateRefreshToken(Long userId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + refreshTokenExpiration);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    /**
     * 토큰에서 회원 ID 추출
     * @param token JWT 토큰
     * @return 회원 ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseClaims(token);
        return Long.parseLong(claims.getSubject());
    }

    /**
     * Access Token에서 토큰 타입 추출
     * @param token JWT Access Token
     * @return TokenType (ACTIVE / REGISTRATION)
     */
    public TokenType getTokenTypeFromToken(String token) {
        Claims claims = parseClaims(token);
        String typeString = claims.get(CLAIM_TOKEN_TYPE, String.class);
        return TokenType.valueOf(typeString);
    }

    /**
     * 토큰 유효성 검증
     * @param token JWT 토큰
     * @return 유효 여부
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("만료된 JWT 토큰입니다.");
        } catch (JwtException e) {
            log.warn("유효하지 않은 JWT 토큰입니다: {}", e.getMessage());
        }
        return false;
    }

    /**
     * 토큰 파싱하여 Claims 추출
     */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
