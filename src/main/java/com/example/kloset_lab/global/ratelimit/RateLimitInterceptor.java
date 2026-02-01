package com.example.kloset_lab.global.ratelimit;

import com.example.kloset_lab.global.exception.CustomException;
import com.example.kloset_lab.global.exception.ErrorCode;
import com.example.kloset_lab.global.security.jwt.JwtAuthenticationToken;
import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Rate Limit 검사 인터셉터
 */
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitBucketManager bucketManager;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
            return true;
        }

        Long userId = jwtAuth.getUserId();
        RateLimitType type = determineRateLimitType(request);

        Bucket bucket = bucketManager.resolveBucket(userId, type);
        if (!bucket.tryConsume(1)) {
            throw new CustomException(ErrorCode.RATE_LIMIT_EXCEEDED);
        }

        return true;
    }

    private RateLimitType determineRateLimitType(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();

        if ("POST".equalsIgnoreCase(method)) {
            if (uri.endsWith("/clothes/analyses") || uri.equals("/api/v1/outfits")) {
                return RateLimitType.AI_API;
            }
        }

        return RateLimitType.GENERAL_API;
    }
}
