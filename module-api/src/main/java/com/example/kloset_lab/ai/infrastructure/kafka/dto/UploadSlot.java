package com.example.kloset_lab.ai.infrastructure.kafka.dto;

/**
 * VTON 이미지 업로드용 슬롯 (presigned URL + fileId)
 *
 * @param presignedUrl S3 PUT용 presigned URL
 * @param fileId MediaFile PK (응답 시 반환하여 상태 전환에 사용)
 */
public record UploadSlot(String presignedUrl, Long fileId) {}
