package com.example.kloset_lab.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/** 채팅 이미지 S3 존재 확인용 S3Client 빈 등록. */
@Configuration
public class ChatS3Config {

    @Bean
    public S3Client s3Client() {
        return S3Client.builder().region(Region.AP_NORTHEAST_2).build();
    }
}
