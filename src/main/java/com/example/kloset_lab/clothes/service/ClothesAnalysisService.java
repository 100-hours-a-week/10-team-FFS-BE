package com.example.kloset_lab.clothes.service;

import com.example.kloset_lab.clothes.dto.ClothesAnalysisResponse;
import com.example.kloset_lab.clothes.dto.ClothesPollingResponse;
import com.example.kloset_lab.clothes.entity.TempClothesBatch;
import com.example.kloset_lab.clothes.entity.TempClothesTask;
import com.example.kloset_lab.clothes.repository.TempClothesBatchRepository;
import com.example.kloset_lab.clothes.repository.TempClothesTaskRepository;
import com.example.kloset_lab.global.ai.http.dto.TaskStatus;
import com.example.kloset_lab.global.ai.http.dto.BatchStatus;
import com.example.kloset_lab.global.ai.http.dto.Meta;
import com.example.kloset_lab.global.ai.http.dto.MajorFeature;
import com.example.kloset_lab.global.ai.kafka.dto.AnalyzeRequest;
import com.example.kloset_lab.global.ai.kafka.dto.AnalyzeResult;
import com.example.kloset_lab.global.ai.kafka.producer.ClothesAnalysisProducer;
import com.example.kloset_lab.global.exception.CustomException;
import com.example.kloset_lab.global.exception.ErrorCode;
import com.example.kloset_lab.media.entity.Purpose;
import com.example.kloset_lab.media.service.MediaService;
import com.example.kloset_lab.user.entity.User;
import com.example.kloset_lab.user.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.stream.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClothesAnalysisService {

    private final UserRepository userRepository;
    private final TempClothesBatchRepository tempClothesBatchRepository;
    private final TempClothesBatchService tempClothesBatchService;
    private final ObjectMapper objectMapper;
    private final MediaService mediaService;

    private final ClothesAnalysisProducer clothesAnalysisProducer;
    private final TempClothesTaskRepository tempClothesTaskRepository;

    @Transactional
    public ClothesAnalysisResponse requestAnalysis(Long currentUserId, List<Long> fileIds) {
        User user =
                userRepository.findById(currentUserId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        mediaService.confirmFileUpload(currentUserId, Purpose.CLOTHES_TEMP, fileIds);

        List<String> imageUrls = mediaService.getFileFullUrls(fileIds);

        String batchId = UUID.randomUUID().toString();

        List<AnalyzeRequest> requests = new ArrayList<>();

        for (int i = 0; i < imageUrls.size(); i++) {
            String taskId = UUID.randomUUID().toString();

            AnalyzeRequest request = new AnalyzeRequest(batchId, taskId, currentUserId, imageUrls.get(i));
            requests.add(request);

            // Kafka로 분석 요청 발행
            clothesAnalysisProducer.requestAnalysis(request);
        }

        // DB 저장
        saveBatchAndTasks(user, batchId, requests);

        return ClothesAnalysisResponse.builder()
                .batchId(batchId)
                .total(imageUrls.size())
                .passed(imageUrls.size())
                .failed(0)
                .build();
    }

    @Transactional
    public void handlePreprocessingCompleted(AnalyzeResult result) {
        TempClothesTask task = tempClothesTaskRepository.findByTaskId(result.taskId()).orElseThrow();
        task.updateFileId(TaskStatus.PREPROCESSING_COMPLETED, result.fileId());
        log.info("[Service] 전처리 완료 - batchId: {}, taskId: {}, fileId: {}",
                result.batchId(), result.taskId(), result.fileId());
    }

    @Transactional
    public void handleAnalysisCompleted(AnalyzeResult result) {
        TempClothesTask task = tempClothesTaskRepository.findByTaskId(result.taskId()).orElseThrow();
        task.updateAnalyzeResult(TaskStatus.ANALYZING_COMPLETED, result.major(), result.extra());

        TempClothesBatch batch = tempClothesBatchRepository.findByBatchId(result.batchId()).orElseThrow();
        batch.completeTask();

        log.info("[Service] 분석 완료 - batchId: {}, taskId: {}, 진행: {}/{}",
                result.batchId(), result.taskId(), batch.getCompleted(), batch.getTotal());
    }

    /**
     * 옷 분석 결과 폴링 API
     */
    @Transactional(readOnly = true)
    public ClothesPollingResponse getAnalysisResult(Long currentUserId, String batchId) {
        TempClothesBatch batch = tempClothesBatchService.findAndValidateBatch(currentUserId, batchId);

        return toPollingResponse(batch);
    }

    private void saveBatchAndTasks(User user, String batchId, List<AnalyzeRequest> requests) {
        TempClothesBatch batch = TempClothesBatch.builder()
                .user(user)
                .batchId(batchId)
                .status(BatchStatus.ACCEPTED)
                .total(requests.size())
                .build();

        for (AnalyzeRequest request : requests) {
            TempClothesTask task = TempClothesTask.builder()
                    .taskId(request.taskId())
                    .status(TaskStatus.REQUESTED_COMPLETED)
                    .build();
            batch.addTask(task);
        }

        tempClothesBatchRepository.save(batch);
    }

    private ClothesPollingResponse toPollingResponse(TempClothesBatch batch) {
        List<Long> fileIds = batch.getTasks().stream()
                .map(TempClothesTask::getFileId)
                .filter(fileId -> fileId != null)
                .distinct()
                .toList();

        Map<Long, String> fileUrlMap = fileIds.isEmpty() ? Map.of() : mediaService.getFileFullUrlsMap(fileIds);

        List<ClothesPollingResponse.TaskResult> results = batch.getTasks().stream()
                .map(task -> toTaskResult(task, fileUrlMap))
                .toList();

        return ClothesPollingResponse.builder()
                .batchId(batch.getBatchId())
                .status(batch.getStatus())
                .meta(Meta.builder()
                        .total(batch.getTotal())
                        .completed(batch.getCompleted())
                        .processing(batch.getProcessing())
                        .isFinished(batch.isFinished())
                        .build())
                .results(results)
                .build();
    }

    private ClothesPollingResponse.TaskResult toTaskResult(TempClothesTask task, Map<Long, String> fileUrlMap) {

        String imageUrl = task.getFileId() != null ? fileUrlMap.get(task.getFileId()) : null;

        MajorFeature major = parseMajorFeature(task.getMajor());

        return ClothesPollingResponse.TaskResult.builder()
                .taskId(task.getTaskId())
                .status(task.getStatus())
                .fileId(task.getFileId())
                .imageUrl(imageUrl)
                .major(major)
                .build();
    }

    private MajorFeature parseMajorFeature(String majorJson) {
        if (majorJson == null) {
            return null;
        }
        try {
            return objectMapper.readValue(majorJson, MajorFeature.class);
        } catch (JsonProcessingException e) {
            log.error("MajorFeature 파싱 실패: {}", majorJson, e);
            return null;
        }
    }
}
