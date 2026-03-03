package com.example.kloset_lab.global.response;

import lombok.Builder;

/**
 * 페이지네이션 정보
 *
 * @param hasNextPage 다음 페이지 존재 여부
 * @param nextCursor 다음 페이지 커서 (다음 페이지가 없으면 null)
 */
@Builder
public record PageInfo(boolean hasNextPage, Long nextCursor) {}
