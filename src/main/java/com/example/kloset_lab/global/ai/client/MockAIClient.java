package com.example.kloset_lab.global.ai.client;

import com.example.kloset_lab.global.ai.dto.*;
import com.github.f4b6a3.ulid.UlidCreator;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!prod")
public class MockAIClient implements AIClient {

    @Override
    public ValidateResponse validateImages(Long userId, List<String> imageUrlList) {
        return ValidateResponse.builder()
                .success(true)
                .validationSummary(ValidateResponse.ValidationSummary.builder()
                        .total(imageUrlList.size())
                        .passed(imageUrlList.size() - 1)
                        .failed(1)
                        .build())
                .validationResults(imageUrlList.stream()
                        .map(url -> ValidateResponse.ValidationResult.builder()
                                .originUrl(url)
                                .passed(true)
                                .build())
                        .toList())
                .build();
    }

    @Override
    public BatchResponse analyzeImages(Long userId, List<String> imageUrlList) {
        return BatchResponse.builder()
                .batchId(UlidCreator.getUlid().toString())
                .status(BatchStatus.ACCEPTED)
                .meta(BatchResponse.Meta.builder()
                        .total(imageUrlList.size())
                        .completed(0)
                        .processing(imageUrlList.size())
                        .isFinished(false)
                        .build())
                .results(List.of(BatchResponse.TaskResult.builder()
                        .taskId(UlidCreator.getUlid().toString())
                        .status(TaskStatus.PREPROCESSING)
                        .fileId(1L)
                        .attributes(BatchResponse.Attributes.builder()
                                .category("상의")
                                .color(List.of("검정"))
                                .material(List.of("면"))
                                .styleTags(List.of("캐주얼", "심플"))
                                .build())
                        .build()))
                .build();
    }

    @Override
    public BatchResponse getBatchStatus(String batchId) {
        // 모든 분석 완료 시나리오
        return BatchResponse.builder()
                .batchId(batchId)
                .status(BatchStatus.COMPLETED)
                .meta(BatchResponse.Meta.builder()
                        .total(3)
                        .completed(1)
                        .processing(2)
                        .isFinished(true)
                        .build())
                .results(List.of(BatchResponse.TaskResult.builder()
                        .taskId(UlidCreator.getUlid().toString())
                        .status(TaskStatus.COMPLETED)
                        .fileId(1L)
                        .attributes(BatchResponse.Attributes.builder()
                                .category("상의")
                                .color(List.of("검정"))
                                .material(List.of("면"))
                                .styleTags(List.of("캐주얼", "심플"))
                                .build())
                        .build()))
                .build();
    }

    @Override
    public EmbeddingResponse saveEmbedding(EmbeddingRequest request) {
        return EmbeddingResponse.builder()
                .clothesId(request.clothesId())
                .caption("Sample Text")
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
                        .items(List.of(1L, 2L))
                        .fileId(1L)
                        .build()))
                .sessionId(null)
                .build();
    }

    @Override
    public ShopResponse searchShop(Long userId, String query) {
        return ShopResponse.builder()
                .querySummary(query + " 검색 결과입니다")
                .outfits(List.of(ShopResponse.ShopOutfit.builder()
                        .outfitId("shop_mock_001")
                        .items(List.of(ShopResponse.ShopItem.builder()
                                .productId("prod_mock_001")
                                .title("목 데이터 상품")
                                .brand("목 브랜드")
                                .price(29000)
                                .imageUrl("https://example.com/mock.jpg")
                                .link("https://example.com/mock")
                                .source("musinsa")
                                .category("상의")
                                .build()))
                        .build()))
                .sessionId(null)
                .build();
    }
}
