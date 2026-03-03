package com.example.kloset_lab.global.response;

import java.util.List;
import lombok.Builder;

/**
 * 페이징 처리된 목록 응답
 *
 * @param items 조회된 항목 목록
 * @param pageInfo 페이지네이션 정보
 */
@Builder
public record PagedResponse<T>(List<T> items, PageInfo pageInfo) {}
