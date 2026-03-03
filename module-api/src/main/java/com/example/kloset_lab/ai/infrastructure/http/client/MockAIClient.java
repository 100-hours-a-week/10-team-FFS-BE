package com.example.kloset_lab.ai.infrastructure.http.client;

import com.example.kloset_lab.ai.entity.BatchStatus;
import com.example.kloset_lab.ai.entity.TaskStatus;
import com.example.kloset_lab.ai.infrastructure.http.dto.*;
import com.example.kloset_lab.media.dto.FileUploadInfo;
import com.example.kloset_lab.media.dto.FileUploadResponse;
import com.example.kloset_lab.media.entity.Purpose;
import com.example.kloset_lab.media.service.MediaService;
import com.github.f4b6a3.ulid.UlidCreator;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MockAIClient implements AIClient {

    private static final long MIN_DELAY_MS = 10_000;
    private static final long MAX_DELAY_MS = 20_000;

    private final MediaService mediaService;

    private final Map<String, BatchInfo> batchStore = new ConcurrentHashMap<>();
    private final Random random = new Random();

    private static class TaskInfo {
        private final long completeAt;
        private final Long fileId;

        private TaskInfo(long completeAt, Long fileId) {
            this.completeAt = completeAt;
            this.fileId = fileId;
        }
    }

    private static class BatchInfo {
        private final Map<String, TaskInfo> tasks = new LinkedHashMap<>();
        private final long createdAt = System.currentTimeMillis();
    }

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

        String batchId = UlidCreator.getUlid().toString();
        BatchInfo batchInfo = new BatchInfo();

        imageUrlList.forEach(url -> {
            String taskId = UlidCreator.getUlid().toString();

            long delay = MIN_DELAY_MS + random.nextLong(MAX_DELAY_MS - MIN_DELAY_MS);
            long completeAt = System.currentTimeMillis() + delay;
            List<FileUploadResponse> fileUploadResponses = mediaService.requestFileUpload(
                    userId,
                    Purpose.CLOTHES,
                    List.of(FileUploadInfo.builder().name("").type("image/png").build()));
            long fileId = fileUploadResponses.get(0).fileId();

            batchInfo.tasks.put(taskId, new TaskInfo(completeAt, fileId));
        });

        batchStore.put(batchId, batchInfo);

        List<BatchResponse.TaskResult> results = batchInfo.tasks.keySet().stream()
                .map(taskId -> BatchResponse.TaskResult.builder()
                        .taskId(taskId)
                        .status(TaskStatus.PREPROCESSING_COMPLETED)
                        .build())
                .toList();

        return BatchResponse.builder()
                .batchId(batchId)
                .status(BatchStatus.ACCEPTED)
                .meta(Meta.builder()
                        .total(batchInfo.tasks.size())
                        .completed(0)
                        .processing(batchInfo.tasks.size())
                        .isFinished(false)
                        .build())
                .results(results)
                .build();
    }

    @Override
    public BatchResponse getBatchStatus(String batchId) {

        BatchInfo batchInfo = batchStore.get(batchId);

        if (batchInfo == null) {
            return BatchResponse.builder()
                    .batchId(batchId)
                    .status(null)
                    .meta(Meta.builder()
                            .total(0)
                            .completed(0)
                            .processing(0)
                            .isFinished(true)
                            .build())
                    .results(List.of())
                    .build();
        }

        long now = System.currentTimeMillis();

        String majorJson =
                """
                {"category":"TOP","color":["검정"],"material":["면"],"styleTags":["캐주얼","심플"]}""";

        String extraJson =
                """
                {"metaData":{"gender":"남녀공용","season":["봄","가을"],"formality":"세미 포멀","fit":"오버핏","occasion":["면접","비즈니스 미팅","출근"]},
                 "caption":"골드 버튼 디테일이 들어간 캐주얼한 스타일의 빨간색 니트입니다."}""";

        int completedCount = 0;
        List<BatchResponse.TaskResult> results = new ArrayList<>();

        for (var entry : batchInfo.tasks.entrySet()) {
            String taskId = entry.getKey();
            TaskInfo taskInfo = entry.getValue();

            boolean completed = now >= taskInfo.completeAt;
            if (completed) {
                completedCount++;
            }

            results.add(
                    completed
                            ? BatchResponse.TaskResult.builder()
                                    .taskId(taskId)
                                    .status(TaskStatus.ANALYZING_COMPLETED)
                                    .fileId(taskInfo.fileId)
                                    .major(majorJson)
                                    .extra(extraJson)
                                    .build()
                            : BatchResponse.TaskResult.builder()
                                    .taskId(taskId)
                                    .status(TaskStatus.PREPROCESSING_COMPLETED)
                                    .build());
        }

        boolean allCompleted = completedCount == batchInfo.tasks.size();

        return BatchResponse.builder()
                .batchId(batchId)
                .status(allCompleted ? BatchStatus.COMPLETED : BatchStatus.IN_PROGRESS)
                .meta(Meta.builder()
                        .total(batchInfo.tasks.size())
                        .completed(completedCount)
                        .processing(batchInfo.tasks.size() - completedCount)
                        .isFinished(allCompleted)
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
