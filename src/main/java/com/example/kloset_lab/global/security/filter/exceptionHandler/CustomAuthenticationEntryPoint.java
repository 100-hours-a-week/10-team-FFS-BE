package com.example.kloset_lab.global.security.filter.exceptionHandler;

import com.example.kloset_lab.global.exception.ErrorCode;
import com.example.kloset_lab.global.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * 인증 실패 시 401 Unauthorized 응답 처리
 *
 * <p>Spring Security 필터 체인에서 발생하는 인증 예외를 처리합니다.
 * GlobalExceptionHandler는 DispatcherServlet 이후에 동작하므로,
 * 필터 체인에서 발생하는 예외는 이 핸들러에서 처리해야 합니다.</p>
 */
@Slf4j
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException, ServletException {

        log.warn("인증 실패: {} - {}", request.getRequestURI(), authException.getMessage());

        ErrorCode errorCode = ErrorCode.AUTHENTICATION_REQUIRED;
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiResponse<Void> apiResponse = ApiResponse.error(errorCode.getStatus().value(), errorCode.getMessage());
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}
