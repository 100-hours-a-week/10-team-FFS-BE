package com.example.kloset_lab.feed.dto;

import com.example.kloset_lab.global.exception.ErrorCode;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 피드 수정 요청 DTO
 *
 * @param content    피드 내용 (null이면 수정 안함, 최대 500자)
 * @param clothesIds 매핑할 옷 ID 배열 (null이면 수정 안함, 빈 배열이면 매핑 삭제, 최대 10개)
 */
public record FeedUpdateRequest(
        @Size(max = 500, message = ErrorCode.Code.CONTENT_TOO_LONG) String content,
        @Size(max = 10, message = ErrorCode.Code.MAXIMUM_10_CLOTHES_MAPPING_ALLOWED) List<Long> clothesIds) {}
