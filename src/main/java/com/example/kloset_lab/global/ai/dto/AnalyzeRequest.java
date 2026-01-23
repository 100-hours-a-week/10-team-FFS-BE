package com.example.kloset_lab.global.ai.dto;

import com.example.kloset_lab.media.dto.FileUploadResponse;
import java.util.List;
import lombok.Builder;

@Builder
public record AnalyzeRequest(Long userId, String batchId, List<ImageInfo> images) {
    @Builder
    public record ImageInfo(Integer sequence, String targetImage, String taskId, FileUploadResponse fileInfo) {}
}
