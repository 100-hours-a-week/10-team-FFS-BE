package com.example.kloset_lab.global.ai.kafka.producer;

import com.example.kloset_lab.global.ai.kafka.dto.AnalyzeRequest;
import com.example.kloset_lab.global.ai.kafka.dto.EventType;
import com.example.kloset_lab.global.ai.kafka.dto.KafkaEvent;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClothesAnalysisProducer {

    private static final String TOPIC = "ai.clothes.analyze.request";

    @Qualifier("clothesAnalysisKafkaTemplate") private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * AI 옷 분석 요청 이벤트 발행
     *
     */
    public void requestAnalysis(AnalyzeRequest event) {

        log.info(
                "[Producer] 옷 분석 요청 전송 - batchId: {}, sourceId: {}, userId: {}",
                event.batchId(),
                event.sourceId(),
                event.userId());
        KafkaEvent kafkaEvent =
                new KafkaEvent<AnalyzeRequest>(EventType.AI_ANALYSIS_REQUESTED, LocalDateTime.now(), event);

        kafkaTemplate.send(TOPIC, kafkaEvent).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Kafka 전송 실패 - sourceId: {}", event.sourceId(), ex);
            }
        });
    }
}
