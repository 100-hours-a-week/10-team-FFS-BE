package com.example.kloset_lab.global.security.config;

import com.example.kloset_lab.auth.infrastructure.kakao.config.KakaoProperties;
import com.example.kloset_lab.global.security.filter.JwtAuthenticationFilter;
import com.example.kloset_lab.global.security.filter.exceptionHandler.CustomAccessDeniedHandler;
import com.example.kloset_lab.global.security.filter.exceptionHandler.CustomAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties({JwtProperties.class, KakaoProperties.class, CorsProperties.class})
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;
    private final CorsProperties corsProperties;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CORS 설정
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // CSRF 비활성화 (JWT 사용 시 불필요)
                .csrf(AbstractHttpConfigurer::disable)

                // 세션 사용 안 함 (Stateless)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 폼 로그인 / HTTP Basic 비활성화
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

                // 엔드포인트 권한 설정
                .authorizeHttpRequests(auth -> auth
                        // health check 허용
                        .requestMatchers("/actuator/**")
                        .permitAll()
                        // 인증 관련 API는 모두 허용
                        .requestMatchers("/api/v1/auth/**")
                        .permitAll()
                        // 회원가입 추가 정보 입력 API (REGISTRATION 토큰 필요 - 컨트롤러에서 TokenType 검증)
                        .requestMatchers("/api/v1/users", "/api/v1/users/validation")
                        .permitAll()
                        // presigend-url 발급
                        .requestMatchers("/api/v1/presigned-url")
                        .permitAll()
                        // 그 외 API는 인증 필요
                        .anyRequest()
                        .authenticated())

                // 인증/인가 예외 처리
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))

                // JWT 필터 등록
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // yml 설정에서 허용할 origin 읽기
        configuration.setAllowedOrigins(corsProperties.getAllowedOrigins());

        // 허용할 HTTP 메서드
        configuration.setAllowedMethods(corsProperties.getAllowedMethods());

        // 허용할 헤더
        configuration.setAllowedHeaders(corsProperties.getAllowedHeaders());

        // 인증 정보 포함 허용
        configuration.setAllowCredentials(corsProperties.getAllowCredentials());

        // preflight 요청 캐시 시간
        configuration.setMaxAge(corsProperties.getMaxAge());

        // 모든 경로에 대해 위 설정 적용
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
