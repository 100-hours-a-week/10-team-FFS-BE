package com.example.kloset_lab.auth.dto;

import lombok.Builder;

/**
 * 기존 회원 카카오 로그인 응답 DTO
 *
 * 이미 가입을 완료한 회원이 로그인할 때 반환되는 응답입니다.
 *
 * - isRegistered: 항상 true (기존 회원이므로)
 * - accessToken: TokenType.ACTIVE 타입의 JWT (일반 서비스 API 접근 가능)
 * - nickname: 회원의 닉네임 (프로필 표시용)
 *
 * 참고: Refresh Token은 Set-Cookie 헤더로 별도 전달됩니다.
 */
@Builder
public record ExistingUserLoginResponse(boolean isRegistered, String accessToken, Long userId)
        implements KakaoLoginResponse {}
