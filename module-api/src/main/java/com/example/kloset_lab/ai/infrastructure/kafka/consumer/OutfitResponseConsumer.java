package com.example.kloset_lab.ai.infrastructure.kafka.consumer;

import com.example.kloset_lab.ai.infrastructure.kafka.dto.OutfitKafkaResponse;
import com.example.kloset_lab.ai.service.OutfitResultService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * outfit-response 토픽 컨슈머: 진행 상태 / 최종 결과 / 실패 메시지 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Profile("!ci")
public class OutfitResponseConsumer {

    private final OutfitResultService outfitResultService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "outfit-response",
            groupId = "outfit_result_worker_group",
            containerFactory = "outfitResultListenerFactory")
    public void consume(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        try {
            log.info("[OutfitConsumer] 메시지 수신 - partition: {}, offset: {}", record.partition(), record.offset());

            OutfitKafkaResponse response = objectMapper.readValue(record.value(), OutfitKafkaResponse.class);

            if (response.isProcessing()) {
                handleProgress(response);
            } else if (response.isSuccess()) {
                outfitResultService.handleSuccess(response);
            } else if (response.isFailed()) {
                outfitResultService.handleFailure(response);
            } else {
                log.warn("[OutfitConsumer] 알 수 없는 status: {} - requestId: {}", response.status(), response.requestId());
            }

            acknowledgment.acknowledge();
            log.info("[OutfitConsumer] offset 커밋 완료 - partition: {}, offset: {}", record.partition(), record.offset());

        } catch (JsonProcessingException e) {
            log.warn("[OutfitConsumer] JSON 파싱 실패, 건너뜀 - offset: {}", record.offset(), e);
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error(
                    "[OutfitConsumer] 메시지 처리 실패 - partition: {}, offset: {}, error: {}",
                    record.partition(),
                    record.offset(),
                    e.getMessage(),
                    e);
        }
    }

    /**
     * 진행 상태 메시지 처리 (WebSocket 푸시)
     *
     * <p>TODO: WebSocket 연동 후 SimpMessagingTemplate으로 클라이언트에 진행 상태 푸시
     */
    private void handleProgress(OutfitKafkaResponse response) {
        log.info(
                "[OutfitConsumer] 진행 상태 - requestId: {}, step: {}, label: {}",
                response.requestId(),
                response.step(),
                response.stepLabel());
    }
}
