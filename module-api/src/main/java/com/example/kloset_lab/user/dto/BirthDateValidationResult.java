package com.example.kloset_lab.user.dto;

/**
 * 생년월일 유효성 검사 결과
 *
 * @param isValid 유효 여부
 * @param message 응답 메시지
 */
public record BirthDateValidationResult(boolean isValid, String message) {}
