package com.example.kloset_lab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class KlosetChatApplication {
    public static void main(String[] args) {
        SpringApplication.run(KlosetChatApplication.class, args);
    }
}
