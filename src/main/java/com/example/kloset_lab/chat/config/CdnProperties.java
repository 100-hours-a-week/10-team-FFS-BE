package com.example.kloset_lab.chat.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "cdn")
public class CdnProperties {

    private final String baseUrl;
}
