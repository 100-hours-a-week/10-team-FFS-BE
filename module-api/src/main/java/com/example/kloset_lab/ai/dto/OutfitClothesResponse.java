package com.example.kloset_lab.ai.dto;

import java.util.List;
import lombok.Builder;

/**
 * 코디 결과별 포함된 옷 상세 정보 응답 DTO
 *
 * @param results 결과 ID별 옷 목록
 */
@Builder
public record OutfitClothesResponse(List<OutfitClothesGroup> results) {

    /**
     * 단일 코디 결과의 옷 목록
     *
     * @param resultId 코디 결과 ID (TpoResult PK)
     * @param clothes 해당 코디에 포함된 옷 정보 리스트
     */
    @Builder
    public record OutfitClothesGroup(Long resultId, List<ClothesDetail> clothes) {}

    /**
     * 옷 상세 정보 (FeedClothesDto와 동일 구조 + category)
     *
     * @param id 옷 ID
     * @param imageUrl 옷 이미지 URL
     * @param name 옷 이름
     * @param price 가격
     * @param category 카테고리 (TOP, BOTTOM, OUTER 등)
     */
    @Builder
    public record ClothesDetail(Long id, String imageUrl, String name, Integer price, String category) {}
}
