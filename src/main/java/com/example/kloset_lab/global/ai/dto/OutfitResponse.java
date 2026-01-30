package com.example.kloset_lab.global.ai.dto;

import java.util.List;
import lombok.Builder;

@Builder
public record OutfitResponse(String querySummary, List<Outfit> outfits, String sessionId) {
    @Builder
    public record Outfit(String outfitId, String description, String fallbackNotice, List<Long> clothesIds) {}
}
