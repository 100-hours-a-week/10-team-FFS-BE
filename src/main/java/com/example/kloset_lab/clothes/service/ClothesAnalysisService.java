package com.example.kloset_lab.clothes.service;

import com.example.kloset_lab.clothes.dto.ClothesAnalysisResponse;
import com.example.kloset_lab.clothes.dto.ClothesPollingResponse;
import com.example.kloset_lab.clothes.entity.TempClothesBatch;
import com.example.kloset_lab.clothes.entity.TempClothesTask;
import com.example.kloset_lab.clothes.repository.TempClothesBatchRepository;
import com.example.kloset_lab.global.ai.client.AIClient;
import com.example.kloset_lab.global.ai.dto.BatchResponse;
import com.example.kloset_lab.global.ai.dto.MajorFeature;
import com.example.kloset_lab.global.ai.dto.Meta;
import com.example.kloset_lab.global.ai.dto.ValidateResponse;
import com.example.kloset_lab.global.exception.CustomException;
import com.example.kloset_lab.global.exception.ErrorCode;
import com.example.kloset_lab.media.entity.Purpose;
import com.example.kloset_lab.media.service.MediaService;
import com.example.kloset_lab.user.entity.User;
import com.example.kloset_lab.user.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClothesAnalysisService {

    private final UserRepository userRepository;
    private final TempClothesBatchRepository tempClothesBatchRepository;
    private final TempClothesBatchService tempClothesBatchService;
    private final AIClient aiClient;
    private final ObjectMapper objectMapper;
    private final MediaService mediaService;

    @Transactional
    public ClothesAnalysisResponse requestAnalysis(Long currentUserId, List<Long> fileIds) {
        User user =
                userRepository.findById(currentUserId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        mediaService.confirmFileUpload(currentUserId, Purpose.CLOTHES_TEMP, fileIds);

        List<String> imageUrls = mediaService.getFileFullUrls(fileIds);
        ValidateResponse validateResponse = aiClient.validateImages(currentUserId, imageUrls);
        BatchResponse batchResponse = aiClient.analyzeImages(currentUserId, validateResponse.getPassedUrls());

        saveBatchAndTasks(user, batchResponse);

        return ClothesAnalysisResponse.builder()
                .batchId(batchResponse.batchId())
                .total(validateResponse.validationSummary().total())
                .passed(validateResponse.validationSummary().passed())
                .failed(validateResponse.validationSummary().failed())
                .build();
    }

    /**
     * 옷 분석 결과 폴링 API
     */
    public ClothesPollingResponse getAnalysisResult(Long currentUserId, String batchId) {
        TempClothesBatch batch = tempClothesBatchService.findAndValidateBatch(currentUserId, batchId);

        if (!batch.isFinished()) {
            BatchResponse batchResponse = aiClient.getBatchStatus(batchId);
            tempClothesBatchService.updateBatchStatus(batchId, batchResponse);
            batch = tempClothesBatchService.findByBatchIdWithTasks(batchId);
        }

        return toPollingResponse(batch);
    }

    private void saveBatchAndTasks(User user, BatchResponse batchResponse) {
        TempClothesBatch batch = TempClothesBatch.builder()
                .user(user)
                .batchId(batchResponse.batchId())
                .status(batchResponse.status())
                .total(batchResponse.meta().total())
                .build();

        if (batchResponse.results() != null) {
            for (BatchResponse.TaskResult result : batchResponse.results()) {
                TempClothesTask task = TempClothesTask.builder()
                        .taskId(result.taskId())
                        .status(result.status())
                        .build();
                batch.addTask(task);
            }
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
