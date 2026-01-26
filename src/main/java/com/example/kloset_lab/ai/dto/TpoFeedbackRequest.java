package com.example.kloset_lab.ai.dto;

import com.example.kloset_lab.ai.entity.Reaction;
import jakarta.validation.constraints.NotNull;

/**
 * TPO 피드백 등록 요청 DTO
 *
 * @param reaction 사용자 반응 (NONE, GOOD, BAD)
 */
public record TpoFeedbackRequest(@NotNull(message = "reaction은 필수입니다.") Reaction reaction) {}
