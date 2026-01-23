package com.example.kloset_lab.global.ai.client;

import com.example.kloset_lab.global.ai.dto.*;
import java.util.List;

public interface AIClient {

    // 이미지 어뷰징 체크
    ValidateResponse validateImages(Long userId, List<String> imageUrlList);

    // 이미지 분석 시작
    BatchResponse analyzeImages(Long userId, List<String> imageUrlList);

    // 분석 상태 조회 (폴링)
    BatchResponse getBatchStatus(String batchId);

    // 임베딩 저장
    EmbeddingResponse saveEmbedding(EmbeddingRequest request);

    // 아이템 삭제
    void deleteClothes(Long clothesId);

    // 코디 추천
    OutfitResponse recommendOutfit(Long userId, String query);

    // 쇼핑 검색
    ShopResponse searchShop(Long userId, String query);
}
