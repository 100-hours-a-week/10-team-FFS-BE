package com.example.kloset_lab.ai.dto;

import java.util.List;
import lombok.Builder;

/**
 * 쇼핑 코디 추천 응답 DTO
 *
 * @param outfitSummary AI가 생성한 코디 요약
 * @param outfits 코디 목록
 */
@Builder
public record ShopRecommendationResponse(String outfitSummary, List<OutfitItem> outfits) {

    /**
     * 개별 코디 항목
     *
     * @param outfitId AI-BE 기준 outfit 식별자
     * @param products 상품 목록
     * @param feedbackId 피드백 레코드 ID
     */
    @Builder
    public record OutfitItem(String outfitId, List<ProductItem> products, Long feedbackId) {}

    /**
     * 개별 상품 정보
     *
     * @param name 상품명
     * @param price 가격
     * @param brandName 브랜드명
     * @param category 카테고리
     * @param imageUrl 상품 이미지 URL
     * @param link 상품 상세 페이지 URL
     */
    @Builder
    public record ProductItem(
            String name, Integer price, String brandName, String category, String imageUrl, String link) {}
}
