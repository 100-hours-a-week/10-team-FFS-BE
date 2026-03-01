package com.example.kloset_lab.chat.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(CdnProperties.class)
public class ChatConfig {}
