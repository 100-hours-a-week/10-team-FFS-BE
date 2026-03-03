package com.example.kloset_lab.loadtest;

import com.example.kloset_lab.global.security.TokenType;
import com.example.kloset_lab.global.security.provider.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 부하테스트용 JWT 토큰 생성 유틸리티
 *
 * <p>실행: ./gradlew test --tests "*.loadtest.TokenGenerator"
 */
@SpringBootTest
@ActiveProfiles("local")
public class TokenGenerator {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("부하테스트용 JWT 토큰 생성")
    void generateToken() {
        Long userId = 1L;
        String token = jwtTokenProvider.generateAccessToken(userId, TokenType.ACTIVE);

        System.out.println("\n========================================");
        System.out.println("k6 실행 명령어:");
        System.out.println("k6 run -e TOKEN=" + token + " k6/scenario-a-read.js");
        System.out.println("========================================\n");
    }
}
