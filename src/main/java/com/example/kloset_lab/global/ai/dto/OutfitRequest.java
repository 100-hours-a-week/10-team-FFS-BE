package com.example.kloset_lab.global.ai.dto;

import com.example.kloset_lab.media.dto.FileUploadResponse;
import java.util.List;
import lombok.Builder;

@Builder
public record OutfitRequest(
        Long userId, String query, String sessionId, Weather weather, List<FileUploadResponse> urls) {
    @Builder
    public record Weather(Integer temperature, String condition) {}
}
