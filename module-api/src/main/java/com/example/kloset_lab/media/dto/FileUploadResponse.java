package com.example.kloset_lab.media.dto;

import lombok.Builder;

@Builder
public record FileUploadResponse(Long fileId, String objectKey, String presignedUrl) {}
