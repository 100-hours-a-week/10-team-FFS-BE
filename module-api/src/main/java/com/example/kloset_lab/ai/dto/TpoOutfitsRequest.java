package com.example.kloset_lab.ai.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

/**
 * TPO 코디 생성 요청 DTO
 *
 * @param content 요청 내용
 */
public record TpoOutfitsRequest(@NotEmpty @Size(min = 2, max = 100) String content) {}
