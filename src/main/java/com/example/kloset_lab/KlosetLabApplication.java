package com.example.kloset_lab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class KlosetLabApplication {
    public static void main(String[] args) {
        SpringApplication.run(KlosetLabApplication.class, args);
    }
}
