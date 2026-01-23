package com.example.kloset_lab.global.ai.dto;

import java.util.List;
import lombok.Builder;

@Builder
public record ValidateResponse(
        Boolean success, ValidationSummary validationSummary, List<ValidationResult> validationResults) {
    @Builder
    public record ValidationSummary(Integer total, Integer passed, Integer failed) {}

    @Builder
    public record ValidationResult(String originUrl, Boolean passed, ValidateError error) {}
}
