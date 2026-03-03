package com.example.kloset_lab.ai.infrastructure.http.dto;

import java.util.List;
import lombok.Builder;

@Builder
public record ValidateRequest(Long userId, List<String> images) {}
