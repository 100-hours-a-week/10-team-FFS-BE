package com.example.kloset_lab.ai.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

/**
 * 쇼핑 코디 추천 요청 DTO
 *
 * @param content 요청 내용 (2~100자)
 */
public record ShopRecommendationRequest(@NotEmpty @Size(min = 2, max = 100) String content) {}
