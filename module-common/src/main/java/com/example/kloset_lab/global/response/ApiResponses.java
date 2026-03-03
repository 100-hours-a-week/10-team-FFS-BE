package com.example.kloset_lab.global.response;

import com.example.kloset_lab.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class ApiResponses {

    public static ResponseEntity<ApiResponse<Void>> ok(String message) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.<Void>builder()
                        .code(200)
                        .message(message)
                        .data(null)
                        .build());
    }

    public static <T> ResponseEntity<ApiResponse<T>> ok(String message, T data) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.<T>builder()
                        .code(200)
                        .message(message)
                        .data(data)
                        .build());
    }

    public static ResponseEntity<ApiResponse<Void>> created(String message) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<Void>builder()
                        .code(201)
                        .message(message)
                        .data(null)
                        .build());
    }

    public static <T> ResponseEntity<ApiResponse<T>> created(String message, T data) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<T>builder()
                        .code(201)
                        .message(message)
                        .data(data)
                        .build());
    }

    public static ResponseEntity<ApiResponse<Void>> accepted(String message) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.<Void>builder()
                        .code(202)
                        .message(message)
                        .data(null)
                        .build());
    }

    public static <T> ResponseEntity<ApiResponse<T>> accepted(String message, T data) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.<T>builder()
                        .code(202)
                        .message(message)
                        .data(data)
                        .build());
    }

    public static ResponseEntity<ApiResponse<Void>> error(ErrorCode errorCode) {
        return ResponseEntity.status(errorCode.getStatus())
                .body(ApiResponse.<Void>builder()
                        .code(errorCode.getStatus().value())
                        .message(errorCode.getMessage())
                        .data(null)
                        .build());
    }
}
