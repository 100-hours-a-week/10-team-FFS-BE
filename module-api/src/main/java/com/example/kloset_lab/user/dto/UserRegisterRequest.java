package com.example.kloset_lab.user.dto;

import com.example.kloset_lab.user.entity.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * 회원가입 후 추가 정보 저장 요청 DTO
 *
 * @param profileFileId 프로필 이미지 파일 ID (선택)
 * @param nickname      닉네임 (필수, 영/한/숫자만, 1~15자)
 * @param birthDate     생년월일 (필수, YYYY-MM-DD)
 * @param gender        성별 (필수, MALE 또는 FEMALE)
 */
public record UserRegisterRequest(
        Long profileFileId,
        @NotBlank(message = "닉네임은 필수입니다.")
                @Size(min = 1, max = 15, message = "닉네임은 1~15자 이내여야 합니다.")
                @Pattern(regexp = "^[a-zA-Z0-9가-힣]+$", message = "닉네임은 영문, 한글, 숫자만 사용 가능합니다.")
                String nickname,
        @NotNull(message = "생년월일은 필수입니다.") LocalDate birthDate,
        @NotNull(message = "성별은 필수입니다.") Gender gender) {}
