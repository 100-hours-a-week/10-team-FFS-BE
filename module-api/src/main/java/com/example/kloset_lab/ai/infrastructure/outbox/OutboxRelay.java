package com.example.kloset_lab.ai.infrastructure.outbox;

import com.example.kloset_lab.ai.entity.TpoOutbox;
import com.example.kloset_lab.ai.entity.TpoOutboxStatus;
import com.example.kloset_lab.ai.infrastructure.kafka.dto.OutfitKafkaRequest;
import com.example.kloset_lab.ai.repository.TpoOutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Outbox Relay: tpo_outbox PENDING 레코드를 읽어 Kafka에 발행하고 PUBLISHED로 전환한다.
 *
 * <p>TX1에서 TpoRequest + TpoOutbox가 원자적으로 저장되므로, Kafka 발행 전 프로세스가 종료되어도
 * 재시작 후 이 릴레이가 PENDING 레코드를 자동으로 재발행한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Profile("!ci")
public class OutboxRelay {

    private static final String TOPIC = "outfit-request";
    private static final int KAFKA_SEND_TIMEOUT_SECONDS = 5;

    @Qualifier("outfitKafkaTemplate") private final KafkaTemplate<String, Object> kafkaTemplate;

    private final TpoOutboxRepository tpoOutboxRepository;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void relay() {
        List<TpoOutbox> pending = tpoOutboxRepository.findTop100ByStatusOrderByCreatedAtAsc(TpoOutboxStatus.PENDING);

        if (pending.isEmpty()) {
            return;
        }

        log.info("[OutboxRelay] PENDING 레코드 {} 건 처리 시작", pending.size());

        for (TpoOutbox outbox : pending) {
            try {
                OutfitKafkaRequest request = objectMapper.readValue(outbox.getPayload(), OutfitKafkaRequest.class);

                kafkaTemplate
                        .send(TOPIC, outbox.getPartitionKey(), request)
                        .get(KAFKA_SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);

                outbox.markPublished();

                log.info("[OutboxRelay] 발행 완료 - requestId: {}", outbox.getRequestId());

            } catch (Exception e) {
                log.warn(
                        "[OutboxRelay] Kafka 발행 실패, 다음 폴링에서 재시도 - requestId: {}, error: {}",
                        outbox.getRequestId(),
                        e.getMessage());
            }
        }
    }
}
