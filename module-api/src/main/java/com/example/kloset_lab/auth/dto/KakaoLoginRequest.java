package com.example.kloset_lab.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 카카오 로그인 요청 DTO
 *
 * 프론트엔드에서 카카오 인가 후 받은 authorization code를 전달합니다.
 *
 * 흐름:
 * 1. 사용자가 카카오 로그인 완료
 * 2. 카카오가 redirect_uri로 code 파라미터와 함께 리다이렉트
 * 3. 프론트엔드가 이 code를 백엔드로 전송
 */
public record KakaoLoginRequest(@NotBlank(message = "인가 코드는 필수입니다.") String authorizationCode) {}
