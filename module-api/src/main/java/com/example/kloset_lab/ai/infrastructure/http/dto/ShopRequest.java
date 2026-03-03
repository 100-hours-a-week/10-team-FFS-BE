package com.example.kloset_lab.ai.infrastructure.http.dto;

import lombok.Builder;

@Builder
public record ShopRequest(Long userId, String query, String sessionId) {}
