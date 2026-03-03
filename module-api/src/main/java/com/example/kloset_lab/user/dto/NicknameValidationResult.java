package com.example.kloset_lab.user.dto;

/**
 * 닉네임 유효성 검사 결과
 *
 * @param isAvailable 사용 가능 여부
 * @param message 응답 메시지
 */
public record NicknameValidationResult(boolean isAvailable, String message) {}
