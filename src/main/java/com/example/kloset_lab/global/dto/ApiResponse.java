package com.example.kloset_lab.global.dto;

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

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder().code(200).message(message).data(data).build();
    }

    public static <T> ApiResponse<T> success(int code, String message, T data) {
        return ApiResponse.<T>builder().code(code).message(message).data(data).build();
    }

    public static ApiResponse<Void> success(String message) {
        return ApiResponse.<Void>builder().code(200).message(message).data(null).build();
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return ApiResponse.<T>builder().code(code).message(message).data(null).build();
    }
}
