package com.example.kloset_lab.global.ai.http.dto;

import lombok.Builder;

@Builder
public record DeleteResponse(Long clothesId, Boolean deleted) {}
