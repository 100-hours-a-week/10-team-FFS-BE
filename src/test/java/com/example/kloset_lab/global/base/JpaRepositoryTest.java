package com.example.kloset_lab.global.base;

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;

/**
 * JPA 슬라이스 테스트 베이스 클래스
 *
 * <p>싱글톤 컨테이너 패턴: MySQL 컨테이너를 JVM당 1회만 기동하여 모든 서브클래스가 공유한다. static 초기화 블록으로 컨테이너를 시작하면 @Testcontainers의 클래스별 재시작 문제를 방지한다. @DataJpaTest의 @Transactional로 각 테스트 후 자동 롤백되어 격리를 보장한다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public abstract class JpaRepositoryTest {

    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");

    static {
        MYSQL.start();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }
}
