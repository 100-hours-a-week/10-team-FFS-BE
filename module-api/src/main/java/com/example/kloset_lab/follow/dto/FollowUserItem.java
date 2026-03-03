package com.example.kloset_lab.follow.dto;

import com.example.kloset_lab.user.dto.UserProfileDto;
import lombok.Builder;

@Builder
public record FollowUserItem(UserProfileDto userProfile, boolean isFollowing) {}
