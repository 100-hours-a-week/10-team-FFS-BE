package com.example.kloset_lab.global.base;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

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
 * 채팅 서버 Controller 슬라이스 테스트 베이스 클래스.
 *
 * <p>module-chat에는 RateLimitBucketManager가 없으므로 해당 mock이 불필요하다.
 */
public abstract class ChatServerControllerTest {

    protected static final Long TEST_USER_ID = 1L;

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    // JwtAuthenticationFilter 빈 생성에 필요
    @MockitoBean
    protected JwtTokenProvider jwtTokenProvider;

    // @EnableJpaAuditing(JpaAuditingConfig)이 JpaMetamodelMappingContext를 요구하나
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
