package com.example.kloset_lab.global.ratelimit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Rate Limit 설정값 바인딩 클래스
 *
 * @param aiApiDailyLimit AI API 일일 호출 제한 (기본값: 50회)
 * @param generalApiMinuteLimit 일반 API 분당 호출 제한 (기본값: 100회)
 */
@Getter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitProperties {

    private final int aiApiDailyLimit;
    private final int generalApiMinuteLimit;
}
