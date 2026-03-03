package com.example.kloset_lab.feed.dto;

import com.example.kloset_lab.global.exception.ErrorCode;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 피드 생성 요청 DTO
 *
 * @param fileIds    업로드한 파일 ID 배열 (필수, 1~5개)
 * @param content    피드 내용 (선택, 최대 500자)
 * @param clothesIds 매핑할 옷 ID 배열 (선택, 최대 10개)
 */
public record FeedCreateRequest(
        @NotEmpty(message = ErrorCode.Code.MINIMUM_1_FILE_ALLOWED)
                @Size(max = 5, message = ErrorCode.Code.MAXIMUM_5_FILES_ALLOWED)
                List<Long> fileIds,
        @Size(max = 500, message = ErrorCode.Code.CONTENT_TOO_LONG) String content,
        @Size(max = 10, message = ErrorCode.Code.MAXIMUM_10_CLOTHES_MAPPING_ALLOWED) List<Long> clothesIds) {}
