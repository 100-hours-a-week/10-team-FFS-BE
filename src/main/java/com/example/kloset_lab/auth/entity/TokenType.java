package com.example.kloset_lab.auth.entity;

/**
 * JWT Access Token 타입
 * - ACTIVE: 가입 완료된 기존 회원용 토큰 (일반 서비스 API 접근 가능)
 * - REGISTRATION: 신규 회원용 임시 토큰 (회원가입 API만 접근 가능)
 */
public enum TokenType {
    ACTIVE,
    REGISTRATION
}
