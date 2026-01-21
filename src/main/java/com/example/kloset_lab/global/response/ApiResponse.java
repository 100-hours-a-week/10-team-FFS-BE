package com.example.kloset_lab.global.response;

import lombok.Builder;
import lombok.Getter;

/**
 * API 공통 응답 형식
 *
 * @param <T> data 필드의 타입
 */
@Getter
@Builder
public class ApiResponse<T> {

    private final int code;
    private final String message;
    private final T data;

    /**
     * Security Filter용 에러 응답 생성
     * DispatcherServlet 이전에 동작하는 Security Filter에서는 ResponseEntity를 반환할 수 없으므로
     * ApiResponse 객체를 직접 생성하여 HttpServletResponse에 작성해야 합니다.
     *
     * @param code 에러 코드
     * @param message 에러 메시지
     * @return ApiResponse 객체
     */
    public static <T> ApiResponse<T> error(int code, String message) {
        return ApiResponse.<T>builder().code(code).message(message).data(null).build();
    }
}
