package com.example.kloset_lab.global.ai.kafka.dto;

import java.time.LocalDateTime;

public record KafkaEvent<T>(EventType eventType, LocalDateTime requestedAt, T data) {}
