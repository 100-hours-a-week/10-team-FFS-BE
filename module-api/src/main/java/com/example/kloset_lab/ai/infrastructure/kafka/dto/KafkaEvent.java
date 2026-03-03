package com.example.kloset_lab.ai.infrastructure.kafka.dto;

import java.time.LocalDateTime;

public record KafkaEvent<T>(EventType eventType, LocalDateTime requestedAt, T data) {}
