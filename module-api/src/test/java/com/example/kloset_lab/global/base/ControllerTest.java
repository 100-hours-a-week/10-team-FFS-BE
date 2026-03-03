package com.example.kloset_lab.global.base;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import com.example.kloset_lab.global.ratelimit.RateLimitBucketManager;
import com.example.kloset_lab.global.security.TokenType;
import com.example.kloset_lab.global.security.jwt.JwtAuthenticationToken;
import com.example.kloset_lab.global.security.provider.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Controller 슬라이스 테스트 베이스 클래스
 *
 * <p>SecurityMockMvcRequestPostProcessors.authentication()으로 JwtAuthenticationToken을 SecurityContext에
 * 직접 주입하여 JWT 필터 체인을 우회한다. 서브클래스는 @WebMvcTest를 선언하고 테스트 대상 서비스를 @MockitoBean으로 추가한다.
 */
public abstract class ControllerTest {

    protected static final Long TEST_USER_ID = 1L;

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    // JwtAuthenticationFilter 빈 생성에 필요 (context 로드 위해 유지)
    @MockitoBean
    protected JwtTokenProvider jwtTokenProvider;

    // WebMvcConfig → RateLimitInterceptor → RateLimitBucketManager 의존 체인 해소
    @MockitoBean
    protected RateLimitBucketManager rateLimitBucketManager;

    // @EnableJpaAuditing(KlosetLabApplication)이 JpaMetamodelMappingContext를 요구하나
    // @WebMvcTest는 JPA 빈을 로드하지 않으므로 Mock으로 대체한다
    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    /**
     * JwtAuthenticationToken을 SecurityContext에 직접 주입하여 인증된 요청으로 만든다.
     *
     * @param request MockMvc 요청 빌더
     * @return authentication post-processor가 적용된 요청 빌더
     */
    protected MockHttpServletRequestBuilder withAuth(MockHttpServletRequestBuilder request) {
        JwtAuthenticationToken token = new JwtAuthenticationToken(
                TEST_USER_ID, TokenType.ACTIVE, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        return request.with(authentication(token)).with(csrf());
    }
}
