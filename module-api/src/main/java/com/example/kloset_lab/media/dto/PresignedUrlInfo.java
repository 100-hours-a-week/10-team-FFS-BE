package com.example.kloset_lab.media.dto;

import lombok.Builder;

@Builder
public record PresignedUrlInfo(String presignedUrl, String objectKey) {}
