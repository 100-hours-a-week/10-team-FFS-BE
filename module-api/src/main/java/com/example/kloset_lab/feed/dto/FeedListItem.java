package com.example.kloset_lab.feed.dto;

import com.example.kloset_lab.user.dto.UserProfileDto;
import lombok.Builder;

/**
 * 피드 목록 조회용 DTO
 *
 * @param feedId          피드 ID
 * @param primaryImageUrl 대표 이미지 URL
 * @param likeCount       좋아요 수
 * @param commentCount    댓글 수
 * @param userProfile     작성자 프로필
 * @param isLiked         좋아요 여부
 */
@Builder
public record FeedListItem(
        Long feedId,
        String primaryImageUrl,
        Long likeCount,
        Long commentCount,
        UserProfileDto userProfile,
        Boolean isLiked) {}
