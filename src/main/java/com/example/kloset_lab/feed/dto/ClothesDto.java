package com.example.kloset_lab.feed.dto;

import lombok.Builder;

/**
 * 피드에 태그된 옷 정보 DTO
 *
 * @param id       옷 ID
 * @param imageUrl 옷 이미지 URL
 * @param name     옷 이름
 * @param price    가격
 */
@Builder
public record ClothesDto(Long id, String imageUrl, String name, Integer price) {}
