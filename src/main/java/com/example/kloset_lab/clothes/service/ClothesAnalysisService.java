package com.example.kloset_lab.clothes.service;

import com.example.kloset_lab.clothes.dto.ClothesAnalysisResponse;
import com.example.kloset_lab.clothes.dto.ClothesPollingResponse;
import com.example.kloset_lab.clothes.entity.TempClothesBatch;
import com.example.kloset_lab.clothes.entity.TempClothesTask;
import com.example.kloset_lab.clothes.repository.TempClothesBatchRepository;
import com.example.kloset_lab.global.ai.client.AIClient;
import com.example.kloset_lab.global.ai.dto.BatchResponse;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClothesAnalysisService {

    private final UserRepository userRepository;
    private final TempClothesBatchRepository tempClothesBatchRepository;
    private final AIClient aiClient;
    private final ObjectMapper objectMapper;
    private final MediaService mediaService;

    @Transactional
    public ClothesAnalysisResponse requestAnalysis(Long currentUserId, List<Long> fileIds) {
        User user =
                userRepository.findById(currentUserId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        mediaService.confirmFileUpload(currentUserId, Purpose.CLOTHES_TEMP, fileIds);

        List<String> imageUrls = mediaService.getFileFullUrl(fileIds);

        // 어뷰징 체크
        ValidateResponse validateResponse = aiClient.validateImages(currentUserId, imageUrls);

        // AI 분석
        BatchResponse batchResponse = aiClient.analyzeImages(currentUserId, validateResponse.getPassedUrls());

        // 분석 결과 저장
        saveBatchAndTasks(user, batchResponse);

        return ClothesAnalysisResponse.builder()
                .batchId(batchResponse.batchId())
                .total(validateResponse.validationSummary().total())
                .passed(validateResponse.validationSummary().passed())
                .failed(validateResponse.validationSummary().failed())
                .build();
    }

    public ClothesPollingResponse getAnalysisResult(Long currentUserId, String batchId) {
        // 최신 상태 업데이트
        BatchResponse batchResponse = aiClient.getBatchStatus(batchId);

        TempClothesBatch batch = tempClothesBatchRepository
                .findByBatchId(batchId)
                .orElseThrow(() -> new CustomException(ErrorCode.CLOTHES_ANALYSIS_RESULT_NOT_FOUND));

        if (!batch.isOwner(currentUserId)) {
            throw new CustomException(ErrorCode.CLOTHES_ANALYSIS_RESULT_DENIED);
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
                        .fileId(result.fileId())
                        .status(result.status())
                        .build();
                batch.addTask(task);
            }
        }

        tempClothesBatchRepository.save(batch);
    }

    private ClothesPollingResponse toPollingResponse(TempClothesBatch batch) {
        List<ClothesPollingResponse.TaskResult> results =
                batch.getTasks().stream().map(this::toTaskResult).toList();

        return ClothesPollingResponse.builder()
                .batchId(batch.getBatchId())
                .status(batch.getStatus())
                .meta(ClothesPollingResponse.Meta.builder()
                        .total(batch.getTotal())
                        .completed(batch.getCompleted())
                        .processing(batch.getProcessing())
                        .isFinished(batch.isFinished())
                        .build())
                .results(results)
                .build();
    }

    private ClothesPollingResponse.TaskResult toTaskResult(TempClothesTask task) {
        String imageUrl = task.getFileId() != null
                ? mediaService.getFileFullUrl(List.of(task.getFileId())).get(0)
                : null;

        ClothesPollingResponse.Analysis analysis = parseAnalysis(task.getAnalysis());

        return ClothesPollingResponse.TaskResult.builder()
                .taskId(task.getTaskId())
                .status(task.getStatus())
                .imageUrl(imageUrl)
                .analysis(analysis)
                .build();
    }

    private ClothesPollingResponse.Analysis parseAnalysis(String analysisJson) {
        if (analysisJson == null) {
            return null;
        }
        try {
            ClothesPollingResponse.Attributes attributes =
                    objectMapper.readValue(analysisJson, ClothesPollingResponse.Attributes.class);
            return ClothesPollingResponse.Analysis.builder()
                    .attributes(attributes)
                    .build();
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
