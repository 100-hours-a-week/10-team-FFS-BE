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
    private final AIClient aiClient;
    private final ObjectMapper objectMapper;
    private final MediaService mediaService;

    @Transactional
    public ClothesAnalysisResponse requestAnalysis(Long currentUserId, List<Long> fileIds) {
        User user =
                userRepository.findById(currentUserId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        mediaService.confirmFileUpload(currentUserId, Purpose.CLOTHES_TEMP, fileIds);

        List<String> imageUrls = mediaService.getFileFullUrls(fileIds);

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

    /**
     * 옷 분석 결과 폴링 API
     *
     * 성능 최적화:
     * 1. 트랜잭션 분리로 DB 커넥션 홀딩 시간 최소화
     * 2. AI 서버 호출 시 커넥션 미사용
     */
    public ClothesPollingResponse getAnalysisResult(Long currentUserId, String batchId) {
        // 1단계: 조회 및 검증 (읽기 전용 트랜잭션, 빠름)
        TempClothesBatch batch = findAndValidateBatch(currentUserId, batchId);

        // 2단계: AI 서버 호출 (트랜잭션 없음, 느려도 커넥션 안 잡고 있음)
        if (!batch.isFinished()) {
            BatchResponse batchResponse = aiClient.getBatchStatus(batchId);

            // 3단계: 업데이트 (쓰기 트랜잭션, 빠름)
            updateBatchStatus(batchId, batchResponse);

            // 4단계: 업데이트된 데이터 다시 조회
            batch = tempClothesBatchRepository
                    .findByBatchId(batchId)
                    .orElseThrow(() -> new CustomException(ErrorCode.CLOTHES_ANALYSIS_RESULT_NOT_FOUND));
        }

        // 5단계: 응답 생성 (N+1 해결된 버전)
        return toPollingResponse(batch);
    }

    /**
     * 배치 조회 및 권한 검증
     * 읽기 전용 트랜잭션으로 빠르게 처리
     */
    @Transactional(readOnly = true)
    public TempClothesBatch findAndValidateBatch(Long currentUserId, String batchId) {
        TempClothesBatch batch = tempClothesBatchRepository
                .findByBatchId(batchId)
                .orElseThrow(() -> new CustomException(ErrorCode.CLOTHES_ANALYSIS_RESULT_NOT_FOUND));

        if (!batch.isOwner(currentUserId)) {
            throw new CustomException(ErrorCode.CLOTHES_ANALYSIS_RESULT_DENIED);
        }

        return batch;
    }

    /**
     * 배치 상태 업데이트
     * 쓰기 트랜잭션으로 분리
     */
    @Transactional
    public void updateBatchStatus(String batchId, BatchResponse batchResponse) {
        TempClothesBatch batch = tempClothesBatchRepository
                .findByBatchId(batchId)
                .orElseThrow(() -> new CustomException(ErrorCode.CLOTHES_ANALYSIS_RESULT_NOT_FOUND));

        updateBatchAndTasks(batch, batchResponse);
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

    private void updateBatchAndTasks(TempClothesBatch batch, BatchResponse batchResponse) {
        batch.updateMeta(
                batchResponse.status(),
                batchResponse.meta().completed(),
                batchResponse.meta().processing(),
                batchResponse.meta().completed() >= batchResponse.meta().total());

        if (batchResponse.results() == null) {
            return;
        }

        for (BatchResponse.TaskResult result : batchResponse.results()) {
            batch.getTasks().stream()
                    .filter(task -> task.getTaskId().equals(result.taskId()))
                    .findFirst()
                    .ifPresent(task -> {
                        task.updateResult(result.status(), result.fileId(), result.major(), result.extra());
                    });
        }
    }

    /**
     * 폴링 응답 생성
     * N+1 문제 해결: fileId를 배치로 조회
     */
    private ClothesPollingResponse toPollingResponse(TempClothesBatch batch) {
        // 1. 모든 fileId 수집
        List<Long> fileIds = batch.getTasks().stream()
                .map(TempClothesTask::getFileId)
                .filter(fileId -> fileId != null)
                .distinct()
                .toList();

        // 2. 한 번에 조회 (N+1 해결!)
        Map<Long, String> fileUrlMap = fileIds.isEmpty() ? Map.of() : mediaService.getFileFullUrlsMap(fileIds);

        // 3. Map에서 URL 가져와서 응답 생성
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

    /**
     * Task를 응답 DTO로 변환
     * fileUrlMap을 받아서 N+1 방지
     */
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
