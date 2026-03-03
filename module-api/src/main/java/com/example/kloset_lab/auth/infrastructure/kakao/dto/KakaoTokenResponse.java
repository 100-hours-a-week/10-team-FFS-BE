package com.example.kloset_lab.auth.infrastructure.kakao.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 카카오 토큰 엔드포인트 응답 DTO
 *
 * 카카오 인증 서버에서 authorization code를 토큰으로 교환할 때 받는 응답입니다.
 * id_token에 사용자의 고유 식별자(sub)가 JWT 형태로 포함되어 있습니다.
 */
public record KakaoTokenResponse(
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("id_token") String idToken,
        @JsonProperty("expires_in") Integer expiresIn,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("refresh_token_expires_in") Integer refreshTokenExpiresIn) {}
