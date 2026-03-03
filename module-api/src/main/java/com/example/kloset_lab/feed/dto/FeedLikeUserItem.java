package com.example.kloset_lab.feed.dto;

import com.example.kloset_lab.user.dto.UserProfileDto;
import lombok.Builder;

/**
 * 피드 좋아요 사용자 목록 아이템 DTO
 *
 * @param userProfile 사용자 프로필 정보
 * @param isFollowing 팔로잉 여부
 */
@Builder
public record FeedLikeUserItem(UserProfileDto userProfile, boolean isFollowing) {}
