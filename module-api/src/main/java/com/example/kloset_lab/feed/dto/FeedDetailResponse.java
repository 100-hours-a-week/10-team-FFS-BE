package com.example.kloset_lab.feed.dto;

import com.example.kloset_lab.user.dto.UserProfileDto;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

/**
 * 피드 상세 조회 응답 DTO
 *
 * @param feedId       피드 ID
 * @param imageUrls    이미지 URL 목록
 * @param likeCount    좋아요 수
 * @param commentCount 댓글 수
 * @param postedTime   게시 시간
 * @param clothes      태그된 옷 목록
 * @param content      피드 내용
 * @param userProfile  작성자 프로필
 * @param isFollowing  팔로잉 여부
 * @param isLiked      좋아요 여부
 * @param isOwner      본인 피드 여부
 */
@Builder
public record FeedDetailResponse(
        Long feedId,
        List<String> imageUrls,
        Long likeCount,
        Long commentCount,
        LocalDateTime postedTime,
        List<FeedClothesDto> clothes,
        String content,
        UserProfileDto userProfile,
        Boolean isFollowing,
        Boolean isLiked,
        Boolean isOwner) {}
