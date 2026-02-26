package com.example.kloset_lab.global.ai.kafka.consumer;

import com.example.kloset_lab.clothes.service.ClothesAnalysisService;
import com.example.kloset_lab.global.ai.kafka.dto.AnalyzeResult;
import com.example.kloset_lab.global.ai.kafka.dto.EventType;
import com.example.kloset_lab.global.ai.kafka.dto.KafkaEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClothesAnalysisConsumer {

    private final ClothesAnalysisService clothesAnalysisService;
    private final ObjectMapper objectMapper;

    /**
     * 이미지 전처리 완료 이벤트 수신
     *
     * containerFactory를 별도 설정하거나,
     * 하나의 리스너에서 Object로 받아 타입 분기하는 방법도 있음.
     * 여기서는 별도 리스너로 분리.
     */
    @KafkaListener(
            topics = "ai.clothes.analyze.result",
            groupId = "ai_analyze_result_worker_group",
            containerFactory = "clothesResultListenerFactory"
    )
    public void consumeAnalyzeResult(ConsumerRecord<String, String> record,
                                     Acknowledgment acknowledgment) {
        try {
            String value = record.value();

            log.info("[Consumer] 메시지 수신 - partition: {}, offset: {}",
                    record.partition(), record.offset());

            KafkaEvent<AnalyzeResult> event =
                        objectMapper.readValue(value, new com.fasterxml.jackson.core.type.TypeReference<KafkaEvent<AnalyzeResult>>() {});

            // event 내용 처리
            EventType eventType = event.eventType();

            switch (eventType) {
                case EventType.AI_ANALYSIS_PREPROCESSING_COMPLETED -> clothesAnalysisService.handlePreprocessingCompleted(event.data());
                case EventType.AI_ANALYSIS_ANALYZING_COMPLETED -> clothesAnalysisService.handleAnalysisCompleted(event.data());
                default -> log.warn("[Consumer] 알 수 없는 eventType: {}", eventType);
            }

            // 처리 성공 시에만 커밋
            acknowledgment.acknowledge();
            log.info("[Consumer] offset 커밋 완료 - partition: {}, offset: {}",
                    record.partition(), record.offset());

        } catch (JsonProcessingException e){
            log.warn("[Consumer] json 형식 에러");
        } catch (Exception e) {
            log.error("[Consumer] 메시지 처리 실패 - partition: {}, offset: {}, error: {}",
                    record.partition(), record.offset(), e.getMessage(), e);
        }
    }
}
