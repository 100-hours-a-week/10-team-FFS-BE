package com.example.kloset_lab.feed.dto;

import lombok.Builder;

/**
 * 좋아요 응답 DTO
 *
 * @param likeCount 좋아요 개수
 * @param isLiked   좋아요 여부
 */
@Builder
public record LikeResponse(long likeCount, boolean isLiked) {}
