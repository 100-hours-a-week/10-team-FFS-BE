package com.example.kloset_lab.global.ai.client;

import com.example.kloset_lab.clothes.entity.Category;
import com.example.kloset_lab.global.ai.dto.*;
import com.github.f4b6a3.ulid.UlidCreator;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!prod")
public class MockAIClient implements AIClient {

    private final List<Long> fileIds = new ArrayList<>();

    @Override
    public ValidateResponse validateImages(Long userId, List<String> imageUrlList) {
        // 어뷰징 전부 통과
        return ValidateResponse.builder()
                .success(true)
                .validationSummary(ValidateResponse.ValidationSummary.builder()
                        .total(imageUrlList.size())
                        .passed(imageUrlList.size())
                        .failed(0)
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
        List<BatchResponse.TaskResult> results = IntStream.rangeClosed(1, imageUrlList.size())
                .mapToObj(i -> BatchResponse.TaskResult.builder()
                        .taskId(String.valueOf(i))
                        .status(TaskStatus.PREPROCESSING)
                        .fileId(null)
                        .major(null)
                        .extra(null)
                        .build())
                .toList();

        return BatchResponse.builder()
                .batchId(UlidCreator.getUlid().toString())
                .status(BatchStatus.ACCEPTED)
                .meta(Meta.builder()
                        .total(imageUrlList.size())
                        .completed(0)
                        .processing(imageUrlList.size())
                        .isFinished(false)
                        .build())
                .results(results)
                .build();
    }

    @Override
    public BatchResponse getBatchStatus(String batchId) {
        String majorJson = """
                {"category":"TOP","color":["검정"],"material":["면"],"styleTags":["캐주얼","심플"]}""";
        String extraJson = """
                {"category":"TOP","color":["검정"],"material":["면"],"styleTags":["캐주얼","심플"]}""";

        List<BatchResponse.TaskResult> results = IntStream.rangeClosed(1, 5)
                .mapToObj(i -> BatchResponse.TaskResult.builder()
                        .taskId(String.valueOf(i))
                        .status(TaskStatus.COMPLETED)
                        .fileId((long) (i))
                        .major(majorJson)
                        .extra(extraJson)
                        .build())
                .toList();

        return BatchResponse.builder()
                .batchId(batchId)
                .status(BatchStatus.COMPLETED)
                .meta(Meta.builder()
                        .total(5)
                        .completed(5)
                        .processing(0)
                        .isFinished(true)
                        .build())
                .results(results)
                .build();
    }

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
                        .items(List.of(1L, 2L))
                        .fileId(1L)
                        .build()))
                .sessionId(null)
                .build();
    }

    @Override
    public ShopResponse searchShop(Long userId, String query) {
        // TODO : V2에서 구현
        return null;
    }
}
