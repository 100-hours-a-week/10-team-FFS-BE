package com.example.kloset_lab.auth.dto;

import com.example.kloset_lab.global.response.Message;

/**
 * 카카오 로그인 응답의 공통 인터페이스
 *
 * 기존 회원(ExistingUserLoginResponse)과 신규 회원(NewUserLoginResponse)의
 * 공통 계약을 정의하여 컨트롤러에서 타입 분기 없이 처리할 수 있도록 합니다.
 */
public sealed interface KakaoLoginResponse permits ExistingUserLoginResponse, NewUserLoginResponse {

    boolean isRegistered();

    String accessToken();

    /**
     * API 응답 메시지 반환
     *
     * @return 기존 회원이면 "login_succeeded", 신규 회원이면 "registration_required"
     */
    default String resultMessage() {
        return isRegistered() ? Message.LOGIN_SUCCEEDED : Message.REGISTRATION_REQUIRED;
    }
}
