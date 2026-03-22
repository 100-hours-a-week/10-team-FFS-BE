package com.example.kloset_lab.ai.entity;

import com.example.kloset_lab.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;

@Entity
@Table(name = "tpo_outbox")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TpoOutbox extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", unique = true, nullable = false, length = 36)
    private String requestId;

    @Column(name = "partition_key", nullable = false)
    private String partitionKey;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "varchar(20)")
    private TpoOutboxStatus status;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static TpoOutbox pending(String requestId, String partitionKey, String payload) {
        TpoOutbox outbox = new TpoOutbox();
        outbox.requestId = requestId;
        outbox.partitionKey = partitionKey;
        outbox.payload = payload;
        outbox.status = TpoOutboxStatus.PENDING;
        return outbox;
    }

    public void markPublished() {
        this.status = TpoOutboxStatus.PUBLISHED;
    }
}
