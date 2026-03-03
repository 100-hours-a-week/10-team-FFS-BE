package com.example.kloset_lab.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 생년월일 유효성 검사 요청 DTO
 *
 * @param birthDate 검사할 생년월일 (yyyy-MM-dd 형식)
 */
public record BirthDateValidationRequest(
        @NotBlank(message = "생년월일은 필수입니다.")
                @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "생년월일 형식은 yyyy-MM-dd 이어야 합니다.")
                String birthDate) {}
