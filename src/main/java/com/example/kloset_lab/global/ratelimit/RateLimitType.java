package com.example.kloset_lab.global.ratelimit;

/**
 * Rate Limit 적용 대상 API 유형
 */
public enum RateLimitType {

    /**
     * AI API (옷 분석, 코디 생성)
     * 일일 50회 제한
     */
    AI_API,

    /**
     * 일반 API
     * 분당 100회 제한
     */
    GENERAL_API
}
