package com.example.kloset_lab.user.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record UserProfiles(List<UserProfileDto> userProfiles) {

}
