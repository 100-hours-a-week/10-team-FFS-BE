package com.example.kloset_lab.ai.infrastructure.http.client;

import com.example.kloset_lab.ai.infrastructure.http.dto.*;
import java.util.*;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MockAIClient implements AIClient {

    @Override
    public EmbeddingResponse saveEmbedding(EmbeddingRequest request) {
        return EmbeddingResponse.builder()
                .clothesId(request.clothesId())
                .indexed(true)
                .build();
    }

    @Override
    public void deleteClothes(Long clothesId) {}

    @Override
    public OutfitResponse recommendOutfit(Long userId, String query) {
        return OutfitResponse.builder()
                .querySummary(query + "에 어울리는 코디입니다")
                .outfits(List.of(OutfitResponse.Outfit.builder()
                        .outfitId("outfit_mock_001")
                        .description("목 데이터 코디 1")
                        .fallbackNotice("에 추가될 fallback notice 입니다.")
                        .clothesIds(List.of(1L, 2L))
                        .build()))
                .sessionId(null)
                .build();
    }

    @Override
    public ShopResponse searchShop(Long userId, String query) {
        return ShopResponse.builder()
                .querySummary(query + "에 어울리는 상품 기반 코디입니다.")
                .outfits(List.of(ShopResponse.ShopOutfit.builder()
                        .outfitId("outfit_mock_s001")
                        .items(List.of(
                                ShopResponse.ShopItem.builder()
                                        .productId("prod_mock_001")
                                        .title("모크 스트릿 티셔츠")
                                        .brand("무신사스탠다드")
                                        .price(29000)
                                        .imageUrl("https://image.musinsa.com/mock/001.jpg")
                                        .link("https://www.musinsa.com/app/goods/001")
                                        .source("musinsa")
                                        .category("상의")
                                        .build(),
                                ShopResponse.ShopItem.builder()
                                        .productId("prod_mock_002")
                                        .title("모크 와이드 데님")
                                        .brand("무신사스탠다드")
                                        .price(45000)
                                        .imageUrl("https://image.musinsa.com/mock/002.jpg")
                                        .link("https://www.musinsa.com/app/goods/002")
                                        .source("musinsa")
                                        .category("하의")
                                        .build(),
                                ShopResponse.ShopItem.builder()
                                        .productId("prod_mock_003")
                                        .title("모크 스니커즈")
                                        .brand("나이퀘")
                                        .price(88000)
                                        .imageUrl("https://image.musinsa.com/mock/003.jpg")
                                        .link("https://www.musinsa.com/app/goods/003")
                                        .source("musinsa")
                                        .category("신발")
                                        .build()))
                        .build()))
                .sessionId(null)
                .build();
    }
}
