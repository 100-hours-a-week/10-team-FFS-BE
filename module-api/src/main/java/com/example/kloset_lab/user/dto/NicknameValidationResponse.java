package com.example.kloset_lab.user.dto;

/**
 * 닉네임 유효성 검사 응답 DTO
 *
 * @param usable 닉네임 사용 가능 여부
 */
public record NicknameValidationResponse(boolean usable) {}
