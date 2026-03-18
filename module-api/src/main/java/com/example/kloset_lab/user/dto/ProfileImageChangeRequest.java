package com.example.kloset_lab.user.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ProfileImageChangeRequest(
        @NotNull(message = "파일 ID는 필수입니다") @Positive(message = "유효하지 않은 파일 ID입니다") Long profileImageFileId) {}
