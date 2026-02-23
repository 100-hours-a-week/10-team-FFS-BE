package com.example.kloset_lab.user.dto;

/**
 * 특정 유저 프로필 조회 응답 DTO
 *
 * @param userProfile 유저 프로필 정보
 * @param isMe        내 프로필 여부
 */
public record UserProfileInfoResponse(
        UserProfileDto userProfile, boolean isMe, long followerCount, long followingCount, boolean isFollowing) {}
