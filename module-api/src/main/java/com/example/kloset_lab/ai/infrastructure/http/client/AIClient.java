package com.example.kloset_lab.ai.infrastructure.http.client;

import com.example.kloset_lab.ai.infrastructure.http.dto.*;

public interface AIClient {

    // 임베딩 저장
    EmbeddingResponse saveEmbedding(EmbeddingRequest request);

    // 아이템 삭제
    void deleteClothes(Long clothesId);

    // 코디 추천
    OutfitResponse recommendOutfit(Long userId, String query);

    // 쇼핑 검색
    ShopResponse searchShop(Long userId, String query);
}
