package com.example.kloset_lab.global.ai.dto;

import java.util.List;
import lombok.Builder;

@Builder
public record ShopResponse(String querySummary, List<ShopOutfit> outfits, String sessionId) {
    @Builder
    public record ShopOutfit(String outfitId, List<ShopItem> items) {}

    @Builder
    public record ShopItem(
            String productId,
            String title,
            String brand,
            Integer price,
            String imageUrl,
            String link,
            String source,
            String category) {}
}
