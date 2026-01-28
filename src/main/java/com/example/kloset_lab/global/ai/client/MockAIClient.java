package com.example.kloset_lab.global.ai.client;

import com.example.kloset_lab.global.ai.dto.*;
import com.github.f4b6a3.ulid.UlidCreator;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!prod")
public class MockAIClient implements AIClient {

    private static final long MIN_DELAY_MS = 10_000;
    private static final long MAX_DELAY_MS = 20_000;

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

            long delay =
                    MIN_DELAY_MS + random.nextLong(MAX_DELAY_MS - MIN_DELAY_MS);
            long completeAt = System.currentTimeMillis() + delay;

            long fileId = 6;

            batchInfo.tasks.put(taskId, new TaskInfo(completeAt, fileId));
        });

        batchStore.put(batchId, batchInfo);

        List<BatchResponse.TaskResult> results = batchInfo.tasks.keySet().stream()
                .map(taskId -> BatchResponse.TaskResult.builder()
                        .taskId(taskId)
                        .status(TaskStatus.PREPROCESSING)
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
                            .status(TaskStatus.COMPLETED)
                            .fileId(taskInfo.fileId)
                            .major(majorJson)
                            .extra(extraJson)
                            .build()
                            : BatchResponse.TaskResult.builder()
                            .taskId(taskId)
                            .status(TaskStatus.PREPROCESSING)
                            .build()
            );
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
