package com.example.kloset_lab.global.security.config;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * CORS 설정 Properties
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "cors")
public class CorsProperties {

    /** 허용할 Origin 목록 */
    private List<String> allowedOrigins;

    /** 허용할 HTTP 메서드 목록 */
    private List<String> allowedMethods;

    /** 허용할 헤더 목록 */
    private List<String> allowedHeaders;

    /** 인증 정보 포함 허용 여부 */
    private Boolean allowCredentials;

    /** preflight 요청 캐시 시간 (초) */
    private Long maxAge;
}
