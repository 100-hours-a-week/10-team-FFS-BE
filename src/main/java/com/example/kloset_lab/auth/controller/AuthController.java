package com.example.kloset_lab.auth.controller;

import com.example.kloset_lab.auth.dto.TokenRefreshResponse;
import com.example.kloset_lab.auth.service.AuthService;
import com.example.kloset_lab.auth.service.AuthService.TokenRefreshResult;
import com.example.kloset_lab.global.exception.InvalidTokenException;
import com.example.kloset_lab.global.response.ApiResponse;
import com.example.kloset_lab.global.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 토큰 갱신 API
     * POST /api/v1/auth/tokens
     */
    @PostMapping("/tokens")
    public ResponseEntity<ApiResponse<TokenRefreshResponse>> refreshToken(
            HttpServletRequest request, HttpServletResponse response) {

        String refreshToken = CookieUtil.extractRefreshToken(request);
        if (refreshToken == null) {
            throw new InvalidTokenException("리프레시 토큰이 없습니다.");
        }

        TokenRefreshResult result = authService.refreshAccessToken(refreshToken);

        // 새 리프레시 토큰을 쿠키에 설정
        CookieUtil.addRefreshTokenCookie(response, result.newRefreshToken());

        return ResponseEntity.ok(ApiResponse.success("accessToken_refreshed", result.response()));
    }

    /**
     * 로그아웃 API
     * DELETE /api/v1/auth/tokens
     */
    @DeleteMapping("/tokens")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal Long userId, HttpServletResponse response) {

        authService.logout(userId);

        // 리프레시 토큰 쿠키 만료 처리
        CookieUtil.expireRefreshTokenCookie(response);

        return ResponseEntity.ok(ApiResponse.success("logout_success"));
    }
}
