package com.example.kloset_lab.global.ai.dto;

import java.util.List;
import lombok.Builder;

@Builder
public record ValidateRequest(Long userId, List<String> images) {}
