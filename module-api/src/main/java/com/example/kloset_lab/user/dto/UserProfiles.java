package com.example.kloset_lab.user.dto;

import java.util.List;
import lombok.Builder;

@Builder
public record UserProfiles(List<UserProfileDto> userProfiles) {}
