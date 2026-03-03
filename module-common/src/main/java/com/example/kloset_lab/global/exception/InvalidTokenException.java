package com.example.kloset_lab.global.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 유효하지 않은 토큰 예외
 * - 토큰이 없거나 만료된 경우
 * - DB에 저장된 토큰과 불일치하는 경우 (탈취 가능성)
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException(String message) {
        super(message);
    }

    public InvalidTokenException() {
        super("유효하지 않은 토큰입니다.");
    }
}
