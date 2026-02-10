package com.example.kloset_lab.ai.dto;

import com.example.kloset_lab.global.ai.dto.ClothesDto;
import java.util.List;
import lombok.Builder;

/**
 * TPO 코디 생성 결과 DTO
 *
 * @param outfitSummary 생성된 코디 요약
 * @param outfits 생성된 코디의 메타 정보 리스트
 */
@Builder
public record TpoOutfitsResponse(String outfitSummary, List<OutfitItem> outfits) {
    @Builder
    public record OutfitItem(Long outfitId, String aiComment, ClothesDto[] clothes, String imageUrl) {}
}
