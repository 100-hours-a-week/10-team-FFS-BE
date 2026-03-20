package com.example.kloset_lab.config;

import com.example.kloset_lab.auth.infrastructure.kakao.config.KakaoProperties;
import com.example.kloset_lab.global.config.LoggingFilter;
import com.example.kloset_lab.global.security.config.CorsProperties;
import com.example.kloset_lab.global.security.config.JwtProperties;
import com.example.kloset_lab.global.security.filter.InternalApiKeyFilter;
import com.example.kloset_lab.global.security.filter.JwtAuthenticationFilter;
import com.example.kloset_lab.global.security.filter.exceptionHandler.CustomAccessDeniedHandler;
import com.example.kloset_lab.global.security.filter.exceptionHandler.CustomAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties({JwtProperties.class, KakaoProperties.class, CorsProperties.class})
@RequiredArgsConstructor
public class ApiSecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;
    private final CorsProperties corsProperties;
    private final LoggingFilter loggingFilter;
    private final InternalApiKeyFilter internalApiKeyFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.requestMatchers("/actuator/health", "/actuator/prometheus")
                        .permitAll()
                        .requestMatchers("/api/v1/auth/**")
                        .permitAll()
                        .requestMatchers("/api/v1/users", "/api/v1/users/validation")
                        .permitAll()
                        .requestMatchers("/api/internal/**")
                        .permitAll()
                        .requestMatchers(
                                "/swagger",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/api-docs",
                                "/api-docs/**",
                                "/v3/api-docs/**")
                        .permitAll()
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/feeds",
                                "/api/v1/feeds/{feedId}",
                                "/api/v1/feeds/{feedId}/likes",
                                "/api/v1/feeds/{feedId}/comments",
                                "/api/v1/feeds/{feedId}/comments/{commentId}/replies",
                                "/api/v1/users/{userId}",
                                "/api/v1/users/{userId}/feeds",
                                "/api/v1/users/{userId}/clothes",
                                "/api/v1/clothes/{clothesId}",
                                "/api/v2/clothes/{clothesId}/feeds",
                                "/api/v1/users/{userId}/**",
                                "/api/v2/users/{userId}/following",
                                "/api/v2/users/{userId}/followers")
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(loggingFilter, CorsFilter.class)
                .addFilterBefore(
                        internalApiKeyFilter,
                        org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(corsProperties.getAllowedOrigins());

        configuration.setAllowedMethods(corsProperties.getAllowedMethods());

        configuration.setAllowedHeaders(corsProperties.getAllowedHeaders());

        configuration.setAllowCredentials(corsProperties.getAllowCredentials());

        configuration.setMaxAge(corsProperties.getMaxAge());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
