package com.example.kloset_lab.global.base;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * MongoDB 슬라이스 테스트 베이스 클래스
 *
 * <p>MongoDB 컨테이너를 JVM당 1회만 기동하여 테스트 클래스 간에 공유한다. @BeforeEach에서 컬렉션을 초기화하여 테스트 격리를 보장한다.
 */
@DataMongoTest
@Testcontainers
public abstract class MongoRepositoryTest {

    @Container
    @ServiceConnection
    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7.0");

    // @EnableJpaAuditing(KlosetLabApplication)이 JpaMetamodelMappingContext를 요구하나
    // @DataMongoTest는 JPA 빈을 로드하지 않으므로 Mock으로 대체한다
    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Autowired
    protected MongoTemplate mongoTemplate;

    @BeforeEach
    void clearCollections() {
        mongoTemplate
                .getDb()
                .listCollectionNames()
                .forEach(name -> mongoTemplate.getDb().getCollection(name).deleteMany(new org.bson.Document()));
    }
}
