package com.example.kloset_lab.ai.infrastructure.kafka.producer;

import com.example.kloset_lab.ai.infrastructure.kafka.dto.OutfitKafkaRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutfitRequestProducer {

    private static final String TOPIC = "outfit-request";

    @Qualifier("outfitKafkaTemplate") private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * 코디 추천 요청을 Kafka에 발행한다.
     * 파티션 키: sessionId (같은 세션의 메시지가 같은 파티션으로 보장)
     *
     * @param request 코디 추천 요청 메시지
     */
    public void send(OutfitKafkaRequest request) {
        log.info(
                "[OutfitProducer] 코디 추천 요청 발행 - requestId: {}, sessionId: {}, userId: {}",
                request.requestId(),
                request.sessionId(),
                request.userId());

        kafkaTemplate.send(TOPIC, request.sessionId(), request).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error(
                        "[OutfitProducer] Kafka 전송 실패 - requestId: {}, error: {}",
                        request.requestId(),
                        ex.getMessage(),
                        ex);
            } else {
                log.info(
                        "[OutfitProducer] Kafka 전송 성공 - requestId: {}, partition: {}, offset: {}",
                        request.requestId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}
