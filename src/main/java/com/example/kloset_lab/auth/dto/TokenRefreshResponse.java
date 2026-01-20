package com.example.kloset_lab.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TokenRefreshResponse {

    private final String accessToken;
}
