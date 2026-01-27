package com.example.kloset_lab.user.dto;

/**
 * 생년월일 유효성 검사 응답 DTO
 *
 * @param valid 생년월일 유효 여부
 */
public record BirthDateValidationResponse(boolean valid) {}
