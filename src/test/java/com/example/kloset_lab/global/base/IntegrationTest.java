package com.example.kloset_lab.global.base;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import com.example.kloset_lab.auth.entity.TokenType;
import com.example.kloset_lab.global.security.jwt.JwtAuthenticationToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * 통합 테스트 베이스 클래스
 *
 * <p>MySQL + MongoDB + Redis Testcontainer를 JVM당 1회 기동하여 모든 서브클래스가 공유한다. 테스트 메서드에 @Transactional을 적용하지
 * 않아 AFTER_COMMIT 이벤트(Redis 캐시 갱신)가 실제 서비스와 동일하게 동작한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
public abstract class IntegrationTest {

    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");
    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7.0");

    @SuppressWarnings("resource")
    static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

    static {
        MYSQL.start();
        MONGO.start();
        REDIS.start();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.data.mongodb.host", MONGO::getHost);
        registry.add("spring.data.mongodb.port", () -> MONGO.getMappedPort(27017));
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    /**
     * 지정한 userId로 인증된 요청 빌더를 반환한다.
     *
     * @param request MockMvc 요청 빌더
     * @param userId  인증 주체 사용자 ID
     * @return authentication post-processor가 적용된 요청 빌더
     */
    protected MockHttpServletRequestBuilder withAuth(MockHttpServletRequestBuilder request, Long userId) {
        JwtAuthenticationToken token =
                new JwtAuthenticationToken(userId, TokenType.ACTIVE, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        return request.with(authentication(token)).with(csrf());
    }
}
