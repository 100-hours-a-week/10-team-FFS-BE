package com.example.kloset_lab.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 닉네임 유효성 검사 요청 DTO
 *
 * @param nickname 검사할 닉네임
 */
public record NicknameValidationRequest(
        @NotBlank(message = "닉네임은 필수입니다.")
                @Size(min = 1, max = 15, message = "닉네임은 1~15자 이내여야 합니다.")
                @Pattern(regexp = "^[a-zA-Z0-9가-힣]+$", message = "닉네임은 영문, 한글, 숫자만 사용 가능합니다.")
                String nickname) {}
