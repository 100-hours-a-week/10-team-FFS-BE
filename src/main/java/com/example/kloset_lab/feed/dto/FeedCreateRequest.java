package com.example.kloset_lab.feed.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 피드 생성 요청 DTO
 *
 * @param fileIds    업로드한 파일 ID 배열 (필수, 1~5개)
 * @param content    피드 내용 (선택)
 * @param clothesIds 매핑할 옷 ID 배열 (선택)
 */
public record FeedCreateRequest(
        @NotEmpty(message = "이미지는 최소 1개 이상 필요합니다") @Size(max = 5, message = "이미지는 최대 5개까지 업로드 가능합니다")
                List<Long> fileIds,
        String content,
        @Size(max = 10, message = "옷 태그는 최대 10개까지 가능합니다") List<Long> clothesIds) {}
