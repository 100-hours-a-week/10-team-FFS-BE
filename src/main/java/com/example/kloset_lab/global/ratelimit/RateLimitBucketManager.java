package com.example.kloset_lab.global.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 사용자별 Rate Limit Bucket 관리
 */
@Component
@RequiredArgsConstructor
public class RateLimitBucketManager {

    private final RateLimitProperties properties;
    private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * 사용자와 Rate Limit 타입에 해당하는 Bucket 반환 (없으면 생성)
     *
     * @param userId 사용자 ID
     * @param type Rate Limit 타입
     * @return Bucket 인스턴스
     */
    public Bucket resolveBucket(Long userId, RateLimitType type) {
        String key = generateKey(userId, type);
        return buckets.computeIfAbsent(key, k -> createBucket(type));
    }

    private String generateKey(Long userId, RateLimitType type) {
        return userId + ":" + type.name();
    }

    private Bucket createBucket(RateLimitType type) {
        Bandwidth bandwidth =
                switch (type) {
                    case AI_API -> Bandwidth.builder()
                            .capacity(properties.getAiApiDailyLimit())
                            .refillGreedy(properties.getAiApiDailyLimit(), Duration.ofDays(1))
                            .build();
                    case GENERAL_API -> Bandwidth.builder()
                            .capacity(properties.getGeneralApiMinuteLimit())
                            .refillGreedy(properties.getGeneralApiMinuteLimit(), Duration.ofMinutes(1))
                            .build();
                };
        return Bucket.builder().addLimit(bandwidth).build();
    }
}
