package com.example.kloset_lab.global.exception;

import com.example.kloset_lab.global.response.ApiResponse;
import com.example.kloset_lab.global.response.ApiResponses;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Bean Validation 실패 시 ErrorCode 기반 응답 반환
     *
     * @param e MethodArgumentNotValidException
     * @return ErrorCode에 매핑된 에러 응답
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        FieldError firstError = e.getBindingResult().getFieldError();
        if (firstError == null) {
            return ApiResponses.error(ErrorCode.INVALID_REQUEST);
        }

        String messageValue = firstError.getDefaultMessage();
        ErrorCode errorCode = findErrorCodeByMessage(messageValue).orElseGet(() -> {
            log.warn("Unknown error code in validation: {}", messageValue);
            return ErrorCode.INVALID_REQUEST;
        });

        log.warn("Validation failed: field={}, errorCode={}", firstError.getField(), errorCode);
        return ApiResponses.error(errorCode);
    }

    /**
     * message 값으로 ErrorCode 조회
     *
     * @param message ErrorCode의 message 필드 값 (snake_case)
     * @return 매칭되는 ErrorCode
     */
    private Optional<ErrorCode> findErrorCodeByMessage(String message) {
        if (message == null) {
            return Optional.empty();
        }
        for (ErrorCode code : ErrorCode.values()) {
            if (code.getMessage().equals(message)) {
                return Optional.of(code);
            }
        }
        return Optional.empty();
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidTokenException(InvalidTokenException e) {
        log.warn("InvalidTokenException: {}", e.getMessage());
        return ApiResponses.error(ErrorCode.INVALID_TOKEN);
    }

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustomException(CustomException e) {
        log.warn("CustomException: {}", e.getMessage());
        return ApiResponses.error(e.getErrorCode());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Unhandled exception: ", e);
        return ApiResponses.error(ErrorCode.INTERNAL_SERVER_ERROR);
    }
}
