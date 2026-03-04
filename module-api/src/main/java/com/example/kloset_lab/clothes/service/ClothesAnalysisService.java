package com.example.kloset_lab.clothes.service;

import com.example.kloset_lab.ai.infrastructure.http.dto.MajorFeature;
import com.example.kloset_lab.ai.infrastructure.kafka.dto.AnalyzeRequest;
import com.example.kloset_lab.ai.infrastructure.kafka.dto.AnalyzeResult;
import com.example.kloset_lab.ai.infrastructure.kafka.producer.ClothesAnalysisProducer;
import com.example.kloset_lab.clothes.dto.ClothesAnalysisResponse;
import com.example.kloset_lab.clothes.dto.ClothesPollingResponse;
import com.example.kloset_lab.clothes.entity.*;
import com.example.kloset_lab.clothes.repository.*;
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
import java.util.stream.Collectors;
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
    private final ObjectMapper objectMapper;
    private final MediaService mediaService;

    private final ClothesAnalysisProducer clothesAnalysisProducer;
    private final TempClothesTaskRepository tempClothesTaskRepository;

    private final AnalyzeBatchRepository analyzeBatchRepository;
    private final AnalyzeSourceRepository analyzeSourceRepository;
    private final AnalyzeTaskRepository analyzeTaskRepository;

    /*
    @Transactional
    public ClothesAnalysisResponse requestAnalysis(Long currentUserId, List<Long> fileIds) {
        User user =
                userRepository.findById(currentUserId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        mediaService.confirmFileUpload(currentUserId, Purpose.CLOTHES_TEMP, fileIds);

        List<String> imageUrls = mediaService.getFileFullUrls(fileIds);

        String batchId = UUID.randomUUID().toString();

        List<AnalyzeRequest> requests = new ArrayList<>();

        for (int i = 0; i < imageUrls.size(); i++) {
            String sourceId = UUID.randomUUID().toString();

            AnalyzeRequest request = new AnalyzeRequest(batchId, sourceId, currentUserId, imageUrls.get(i));
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

     */

    @Transactional
    public ClothesAnalysisResponse requestAnalysis(Long currentUserId, List<Long> fileIds) {
        User user =
                userRepository.findById(currentUserId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        mediaService.confirmFileUpload(currentUserId, Purpose.CLOTHES_TEMP, fileIds);

        List<String> imageUrls = mediaService.getFileFullUrls(fileIds);

        AnalyzeBatch batch = new AnalyzeBatch(currentUserId, imageUrls.size());
        analyzeBatchRepository.save(batch);

        List<AnalyzeSource> sources = new ArrayList<>();
        for (int i = 0; i < imageUrls.size(); i++) {
            AnalyzeSource source = new AnalyzeSource(batch);
            sources.add(source);
        }
        analyzeSourceRepository.saveAll(sources);

        for (int i = 0; i < sources.size(); i++) {
            AnalyzeRequest request = new AnalyzeRequest(
                    batch.getBatchId(), sources.get(i).getSourceId(), currentUserId, imageUrls.get(i));
            clothesAnalysisProducer.requestAnalysis(request);
        }

        return ClothesAnalysisResponse.builder()
                .batchId(batch.getBatchId())
                .total(imageUrls.size())
                .build();
    }

    @Transactional
    public void handleAbusingCompleted(AnalyzeResult result) {
        AnalyzeSource source =
                analyzeSourceRepository.findBySourceId(result.sourceId()).orElseThrow();

        source.completeAbuseCheck(result.passed());
        log.info(
                "[Service] 어뷰징 체크 완료 - batchId: {}, sourceId: {}, passed: {}",
                result.batchId(),
                result.sourceId(),
                result.passed());
    }

    @Transactional
    public void handleSegmentationCompleted(AnalyzeResult result) {
        AnalyzeSource source =
                analyzeSourceRepository.findBySourceId(result.sourceId()).orElseThrow();

        source.completePreprocessing(result.segmentation());
        log.info(
                "[Service] 세그멘테이션 완료 - batchId: {}, sourceId: {}, segmentation: {}",
                result.batchId(),
                result.sourceId(),
                result.segmentation());
    }

    @Transactional
    public void handlePreprocessingCompleted(AnalyzeResult result) {
        System.out.println(result.sourceId());
        AnalyzeSource source =
                analyzeSourceRepository.findBySourceId(result.sourceId()).orElseThrow();

        AnalyzeTask task = new AnalyzeTask(result.taskId(), source, result.fileId());
        analyzeTaskRepository.save(task);
        log.info(
                "[Service] 전처리 완료 - batchId: {}, sourceId: {}, taskId: {}, fileId: {}",
                result.batchId(),
                result.sourceId(),
                result.taskId(),
                result.fileId());
    }

    @Transactional
    public void handleAnalysisCompleted(AnalyzeResult result) {
        System.out.println(result.taskId());
        AnalyzeTask task = analyzeTaskRepository.findByTaskId(result.taskId()).orElseThrow();
        task.completeAnalysis(result.major(), result.extra());
        log.info(
                "[Service] 분석 완료 - batchId: {}, sourceId: {}, taskId: {}",
                result.batchId(),
                result.sourceId(),
                result.taskId());
    }

    /**
     * 옷 분석 결과 폴링 API
     */
    @Transactional(readOnly = true)
    public ClothesPollingResponse getAnalysisResult(Long currentUserId, String batchId) {
        AnalyzeBatch batch = analyzeBatchRepository
                .findByBatchId(batchId)
                .orElseThrow(() -> new CustomException(ErrorCode.CLOTHES_ANALYSIS_RESULT_NOT_FOUND));

        if (!batch.getUserId().equals(currentUserId)) {
            throw new CustomException(ErrorCode.CLOTHES_ANALYSIS_RESULT_NOT_FOUND);
        }

        List<AnalyzeSource> sources = analyzeSourceRepository.findAllByBatchWithBatch(batch);

        // task를 한 번에 조회 후 sourceId로 그룹핑
        List<AnalyzeTask> allTasks = analyzeTaskRepository.findAllBySourceIn(sources);
        Map<Long, List<AnalyzeTask>> tasksBySourceId = allTasks.stream()
                .collect(Collectors.groupingBy(task -> task.getSource().getId()));

        List<ClothesPollingResponse.SourceResult> sourceResults = sources.stream()
                .map(source -> {
                    List<ClothesPollingResponse.TaskResult> taskResults = null;

                    if (source.getStatus() == SourceStatus.PREPROCESSING_COMPLETED) {
                        List<AnalyzeTask> tasks = tasksBySourceId.getOrDefault(source.getId(), List.of());
                        taskResults = tasks.stream()
                                .map(task -> ClothesPollingResponse.TaskResult.builder()
                                        .taskId(task.getTaskId())
                                        .status(task.getStatus())
                                        .fileId(task.getFileId())
                                        .imageUrl(mediaService.getFileFullUrl(task.getFileId()))
                                        .major(
                                                task.getStatus() == TaskStatus.ANALYZING_COMPLETED
                                                        ? parseMajorFeature(task.getMajor())
                                                        : null)
                                        .build())
                                .toList();
                    }

                    return ClothesPollingResponse.SourceResult.builder()
                            .sourceId(source.getSourceId())
                            .status(source.getStatus())
                            .passed(source.getPassed())
                            .detectedCount(source.getDetectedCount())
                            .tasks(taskResults)
                            .build();
                })
                .toList();

        return ClothesPollingResponse.builder()
                .batchId(batch.getBatchId())
                .status(batch.getStatus())
                .results(sourceResults)
                .build();
    }

    private MajorFeature parseMajorFeature(String majorJson) {
        try {
            return objectMapper.readValue(majorJson, MajorFeature.class);
        } catch (JsonProcessingException e) {
            log.warn("major 파싱 실패: {}", e.getMessage());
            return null;
        }
    }
}
