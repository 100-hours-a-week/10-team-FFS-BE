package com.example.kloset_lab.user.dto;

/**
 * 유저 프로필 정보 DTO
 *
 * @param userId              유저 ID
 * @param userProfileImageUrl 프로필 이미지 URL
 * @param nickname            닉네임
 */
public record UserProfileDto(Long userId, String userProfileImageUrl, String nickname) {}
