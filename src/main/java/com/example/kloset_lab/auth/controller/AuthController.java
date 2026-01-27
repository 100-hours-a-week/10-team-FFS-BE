package com.example.kloset_lab.auth.controller;

import com.example.kloset_lab.auth.dto.KakaoLoginRequest;
import com.example.kloset_lab.auth.dto.KakaoLoginResponse;
import com.example.kloset_lab.auth.dto.TokenRefreshResponse;
import com.example.kloset_lab.auth.service.AuthService;
import com.example.kloset_lab.auth.service.AuthService.KakaoLoginResult;
import com.example.kloset_lab.auth.service.AuthService.TokenRefreshResult;
import com.example.kloset_lab.global.exception.InvalidTokenException;
import com.example.kloset_lab.global.response.ApiResponse;
import com.example.kloset_lab.global.response.ApiResponses;
import com.example.kloset_lab.global.response.Message;
import com.example.kloset_lab.global.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 카카오 소셜로그인 API
     * POST /api/v1/auth/kakao
     *
     * 프론트엔드에서 카카오 인가 후 받은 authorization code를 전달받아 로그인 처리합니다.
     *
     * - 기존 회원: isRegistered=true, accessToken, nickname 반환 + Refresh Token 쿠키 설정
     * - 신규 회원: isRegistered=false, accessToken 반환 (회원가입 추가 정보 입력 필요)
     */
    @PostMapping("/kakao")
    public ResponseEntity<ApiResponse<KakaoLoginResponse>> kakaoLogin(
            @RequestBody @Valid KakaoLoginRequest request, HttpServletResponse response) {

        KakaoLoginResult result = authService.kakaoLogin(request.authorizationCode());

        Optional.ofNullable(result.refreshToken())
                .ifPresent(token -> CookieUtil.addRefreshTokenCookie(response, token));

        return ApiResponses.ok(result.response().resultMessage(), result.response());
    }

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

        return ApiResponses.ok(Message.ACCESS_TOKEN_REFRESHED, result.response());
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

        return ApiResponses.ok(Message.LOGOUT_SUCCEEDED);
    }
}
