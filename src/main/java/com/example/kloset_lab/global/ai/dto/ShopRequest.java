package com.example.kloset_lab.global.ai.dto;

import lombok.Builder;

@Builder
public record ShopRequest(Long userId, String query, String sessionId) {}
