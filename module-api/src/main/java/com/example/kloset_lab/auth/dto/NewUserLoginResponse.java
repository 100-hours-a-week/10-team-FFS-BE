package com.example.kloset_lab.auth.dto;

import lombok.Builder;

/**
 * 신규 회원 카카오 로그인 응답 DTO
 *
 * 처음 카카오 로그인하는 신규 회원에게 반환되는 응답입니다.
 * 프론트엔드는 이 응답을 받으면 회원가입 추가 정보 입력 화면으로 이동해야 합니다.
 *
 * - isRegistered: 항상 false (신규 회원이므로)
 * - accessToken: TokenType.REGISTRATION 타입의 JWT (회원가입 API만 접근 가능)
 *
 * 참고: 신규 회원에게는 Refresh Token 쿠키가 발급되지 않습니다.
 *       회원가입 완료 후 다시 로그인해야 Refresh Token을 받을 수 있습니다.
 */
@Builder
public record NewUserLoginResponse(boolean isRegistered, String accessToken) implements KakaoLoginResponse {}
