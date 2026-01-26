package com.example.kloset_lab.auth.service;

import com.example.kloset_lab.auth.dto.ExistingUserLoginResponse;
import com.example.kloset_lab.auth.dto.KakaoLoginResponse;
import com.example.kloset_lab.auth.dto.NewUserLoginResponse;
import com.example.kloset_lab.auth.dto.TokenRefreshResponse;
import com.example.kloset_lab.auth.entity.RefreshToken;
import com.example.kloset_lab.auth.entity.TokenType;
import com.example.kloset_lab.auth.infrastructure.kakao.client.KakaoOAuthClient;
import com.example.kloset_lab.auth.repository.RefreshTokenRepository;
import com.example.kloset_lab.global.exception.InvalidTokenException;
import com.example.kloset_lab.global.security.provider.JwtTokenProvider;
import com.example.kloset_lab.user.entity.Provider;
import com.example.kloset_lab.user.entity.User;
import com.example.kloset_lab.user.entity.UserProfile;
import com.example.kloset_lab.user.repository.UserProfileRepository;
import com.example.kloset_lab.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final KakaoOAuthClient kakaoOAuthClient;

    /**
     * 카카오 로그인 결과
     *
     * @param response 로그인 응답 (기존 회원 또는 신규 회원)
     * @param refreshToken 리프레시 토큰 (기존 회원만 존재, 신규 회원은 null)
     */
    public record KakaoLoginResult(KakaoLoginResponse response, String refreshToken) {}

    /**
     * 카카오 소셜로그인 처리
     *
     * @param authorizationCode 프론트엔드에서 전달받은 카카오 인가 코드
     * @return KakaoLoginResult (기존 회원은 refreshToken 포함, 신규 회원은 null)
     */
    @Transactional
    public KakaoLoginResult kakaoLogin(String authorizationCode) {
        String providerId = kakaoOAuthClient.getProviderId(authorizationCode);

        return userRepository
                .findByProviderAndProviderId(Provider.KAKAO, providerId)
                .map(user -> {
                    if (!user.isRegistrationComplete()) {
                        return handlePendingUser(user);
                    }
                    return handleExistingUser(user);
                })
                .orElseGet(() -> handleNewUser(providerId));
    }

    /**
     * PENDING 상태 회원의 재진입 처리
     *
     * - 회원가입 도중 이탈 후 다시 인가 코드로 돌아온 경우
     * - 기존 User 레코드를 재활용하여 새 REGISTRATION 토큰 발급
     * - Refresh Token은 발급하지 않음
     *
     * @param user PENDING 상태의 기존 User
     * @return 신규 회원과 동일한 응답 (isRegistered=false, REGISTRATION 토큰)
     */
    private KakaoLoginResult handlePendingUser(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), TokenType.REGISTRATION);
        log.info("PENDING 회원 재진입 - userId: {}, 회원가입 재시도", user.getId());
        return createNewUserLoginResult(accessToken);
    }

    /**
     * 기존 회원 로그인 처리
     *
     * - Access Token (ACTIVE) 발급: 일반 서비스 API 접근 가능
     * - Refresh Token 발급 및 DB 저장
     * - UserProfile에서 nickname 조회
     */
    private KakaoLoginResult handleExistingUser(User user) {
        Long userId = user.getId();

        String accessToken = jwtTokenProvider.generateAccessToken(userId, TokenType.ACTIVE);
        String refreshToken = jwtTokenProvider.generateRefreshToken(userId);

        refreshTokenRepository.deleteByUserId(userId);
        refreshTokenRepository.save(
                RefreshToken.builder().user(user).tokenString(refreshToken).build());

        String nickname = userProfileRepository
                .findByUserId(userId)
                .map(UserProfile::getNickname)
                .orElseThrow(() -> new IllegalStateException("기존 회원이지만 프로필이 없습니다. userId: " + userId));

        log.info("기존 회원 카카오 로그인 완료 - userId: {}, nickname: {}", userId, nickname);

        return createExistingUserLoginResult(accessToken, refreshToken, userId);
    }

    /**
     * 신규 회원 처리
     *
     * - User 엔티티 생성 (status: PENDING)
     * - Access Token (REGISTRATION) 발급: 회원가입 API만 접근 가능
     * - Refresh Token은 발급하지 않음
     */
    private KakaoLoginResult handleNewUser(String providerId) {
        User newUser =
                User.builder().provider(Provider.KAKAO).providerId(providerId).build();
        userRepository.save(newUser);

        String accessToken = jwtTokenProvider.generateAccessToken(newUser.getId(), TokenType.REGISTRATION);
        log.info("신규 회원 카카오 로그인 - userId: {}, 회원가입 필요", newUser.getId());

        return createNewUserLoginResult(accessToken);
    }

    /**
     * 토큰 갱신 (RTR: Refresh Token Rotation 적용)
     *
     * @param refreshTokenString 쿠키에서 추출한 리프레시 토큰 문자열
     * @return 새로운 Access Token과 Refresh Token
     */
    @Transactional
    public TokenRefreshResult refreshAccessToken(String refreshTokenString) {
        // 1. 토큰 유효성 검증
        if (!jwtTokenProvider.validateToken(refreshTokenString)) {
            throw new InvalidTokenException("유효하지 않거나 만료된 리프레시 토큰입니다. 재로그인이 필요합니다.");
        }

        // 2. 토큰에서 userId 추출
        Long userId = jwtTokenProvider.getUserIdFromToken(refreshTokenString);

        // 3. DB에서 userId로 저장된 토큰 조회
        RefreshToken savedToken = refreshTokenRepository
                .findByUserId(userId)
                .orElseThrow(() -> new InvalidTokenException("저장된 리프레시 토큰이 없습니다. 재로그인이 필요합니다."));

        // 4. 토큰 문자열 일치 여부 확인 (탈취 감지)
        if (!savedToken.getTokenString().equals(refreshTokenString)) {
            // 토큰 불일치 - 탈취 가능성, 해당 유저의 모든 토큰 삭제
            refreshTokenRepository.deleteAllByUserId(userId);
            throw new InvalidTokenException("토큰이 일치하지 않습니다. 보안을 위해 재로그인이 필요합니다.");
        }

        // 5. User 조회
        User user = savedToken.getUser();

        // 6. 새로운 토큰 생성
        String newAccessToken = jwtTokenProvider.generateAccessToken(userId, TokenType.ACTIVE);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(userId);

        // 7. 기존 토큰 삭제 후 새 토큰 저장 (RTR)
        refreshTokenRepository.delete(savedToken);
        refreshTokenRepository.save(
                RefreshToken.builder().user(user).tokenString(newRefreshToken).build());

        log.info("토큰 갱신 완료 - userId: {}", userId);

        return new TokenRefreshResult(
                TokenRefreshResponse.builder().accessToken(newAccessToken).build(), newRefreshToken);
    }

    /**
     * 로그아웃 처리
     *
     * @param userId SecurityContext에서 추출한 회원 ID
     */
    @Transactional
    public void logout(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
        log.info("로그아웃 완료 - userId: {}", userId);
    }

    /**
     * 토큰 갱신 결과를 담는 내부 클래스
     */
    public record TokenRefreshResult(TokenRefreshResponse response, String newRefreshToken) {}

    /**
     * 신규/PENDING 회원용 응답 생성
     *
     * @param accessToken REGISTRATION 토큰
     * @return KakaoLoginResult (refreshToken은 null)
     */
    private KakaoLoginResult createNewUserLoginResult(String accessToken) {
        NewUserLoginResponse response = NewUserLoginResponse.builder()
                .isRegistered(false)
                .accessToken(accessToken)
                .build();
        return new KakaoLoginResult(response, null);
    }

    /**
     * 기존 회원용 응답 생성
     *
     * @param accessToken ACTIVE 토큰
     * @param refreshToken Refresh 토큰
     * @param nickname 회원 닉네임
     * @return KakaoLoginResult
     */
    private KakaoLoginResult createExistingUserLoginResult(String accessToken, String refreshToken, Long userId) {
        ExistingUserLoginResponse response = ExistingUserLoginResponse.builder()
                .isRegistered(true)
                .accessToken(accessToken)
                .userId(userId)
                .build();
        return new KakaoLoginResult(response, refreshToken);
    }
}
