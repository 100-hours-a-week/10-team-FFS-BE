package com.example.kloset_lab.global.ai.dto;

import lombok.Builder;

@Builder
public record Meta(Integer total, Integer completed, Integer processing, Boolean isFinished) {}
