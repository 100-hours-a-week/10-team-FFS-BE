package com.example.kloset_lab.auth.infrastructure.kakao.client;

import com.example.kloset_lab.auth.infrastructure.kakao.config.KakaoProperties;
import com.example.kloset_lab.auth.infrastructure.kakao.dto.KakaoTokenResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Base64;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * 카카오 OAuth 클라이언트
 *
 * 카카오 인증 서버와의 HTTP 통신을 담당합니다.
 * - Authorization Code를 토큰으로 교환
 * - id_token에서 사용자 고유 ID(sub) 추출
 */
@Slf4j
@Component
public class KakaoOAuthClient {

    private final KakaoProperties kakaoProperties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public KakaoOAuthClient(KakaoProperties kakaoProperties, ObjectMapper objectMapper) {
        this.kakaoProperties = kakaoProperties;
        this.restClient = RestClient.create();
        this.objectMapper = objectMapper;
    }

    /**
     * Authorization Code로 카카오 사용자의 고유 ID(providerId)를 조회합니다.
     *
     * 흐름:
     * 1. Authorization Code를 카카오 토큰 엔드포인트로 전송
     * 2. 받은 id_token(JWT)을 파싱하여 sub 클레임 추출
     *
     * @param authorizationCode 프론트엔드에서 전달받은 카카오 인가 코드
     * @return 카카오 사용자 고유 ID (sub 클레임 값)
     */
    public String getProviderId(String authorizationCode) {
        KakaoTokenResponse tokenResponse = requestToken(authorizationCode);

        return extractSubFromIdToken(tokenResponse.idToken());
    }

    /**
     * Authorization Code를 카카오 토큰으로 교환합니다.
     *
     * POST https://kauth.kakao.com/oauth/token
     * Content-Type: application/x-www-form-urlencoded
     */
    private KakaoTokenResponse requestToken(String authorizationCode) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "authorization_code");
        formData.add("client_id", kakaoProperties.getClientId());
        formData.add("client_secret", kakaoProperties.getClientSecret());
        formData.add("redirect_uri", kakaoProperties.getRedirectUri());
        formData.add("code", authorizationCode);

        KakaoTokenResponse response = restClient
                .post()
                .uri(kakaoProperties.getTokenUri())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .retrieve()
                .body(KakaoTokenResponse.class);

        log.debug("카카오 토큰 교환 성공");
        return response;
    }

    /**
     * id_token(JWT)에서 sub 클레임을 추출합니다.
     *
     * JWT 구조: header.payload.signature
     * payload를 Base64 디코딩하여 sub 값을 추출합니다.
     */
    private String extractSubFromIdToken(String idToken) {
        try {
            String[] parts = idToken.split("\\.");
            if (parts.length != 3) {
                throw new IllegalArgumentException("유효하지 않은 id_token 형식입니다.");
            }

            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));

            JsonNode jsonNode = objectMapper.readTree(payload);
            String sub = jsonNode.get("sub").asText();

            log.debug("카카오 사용자 ID(sub) 추출 성공");
            return sub;

        } catch (Exception e) {
            log.error("id_token 파싱 실패: {}", e.getMessage());
            throw new RuntimeException("카카오 id_token 파싱에 실패했습니다.", e);
        }
    }
}
