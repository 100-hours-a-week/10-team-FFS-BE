package com.example.kloset_lab.clothes.service;

import com.example.kloset_lab.clothes.entity.TempClothesBatch;
import com.example.kloset_lab.clothes.repository.TempClothesBatchRepository;
import com.example.kloset_lab.global.ai.dto.BatchResponse;
import com.example.kloset_lab.global.exception.CustomException;
import com.example.kloset_lab.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TempClothesBatchService {

    private final TempClothesBatchRepository tempClothesBatchRepository;

    /**
     * 배치 조회 (tasks 포함)
     */
    @Transactional(readOnly = true)
    public TempClothesBatch findByBatchIdWithTasks(String batchId) {
        return tempClothesBatchRepository
                .findByBatchIdWithTasks(batchId)
                .orElseThrow(() -> new CustomException(ErrorCode.CLOTHES_ANALYSIS_RESULT_NOT_FOUND));
    }

    /**
     * 배치 조회 및 권한 검증
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
     */
    @Transactional
    public void updateBatchStatus(String batchId, BatchResponse batchResponse) {
        TempClothesBatch batch = tempClothesBatchRepository
                .findByBatchIdWithTasks(batchId)
                .orElseThrow(() -> new CustomException(ErrorCode.CLOTHES_ANALYSIS_RESULT_NOT_FOUND));

        updateBatchAndTasks(batch, batchResponse);
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
}
