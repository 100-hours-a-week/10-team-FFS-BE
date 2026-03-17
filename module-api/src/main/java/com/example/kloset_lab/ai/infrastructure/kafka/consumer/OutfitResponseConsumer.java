package com.example.kloset_lab.ai.infrastructure.kafka.consumer;

import com.example.kloset_lab.ai.dto.OutfitResultContext;
import com.example.kloset_lab.ai.dto.OutfitResultContext.OutfitSummary;
import com.example.kloset_lab.ai.entity.TpoRequest;
import com.example.kloset_lab.ai.infrastructure.kafka.dto.OutfitKafkaResponse;
import com.example.kloset_lab.ai.repository.TpoRequestRepository;
import com.example.kloset_lab.ai.service.OutfitResultService;
import com.example.kloset_lab.global.infrastructure.OutfitWebSocketMessage;
import com.example.kloset_lab.global.infrastructure.OutfitWebSocketMessage.OutfitData;
import com.example.kloset_lab.global.infrastructure.RedisEventPublisher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
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

    private static final String OUTFIT_CHANNEL = "outfit:event:%d";

    private final OutfitResultService outfitResultService;
    private final TpoRequestRepository tpoRequestRepository;
    private final RedisEventPublisher redisEventPublisher;
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
                handleSuccess(response);
            } else if (response.isFailed()) {
                handleFailure(response);
            } else if (response.isClarificationNeeded()) {
                handleClarificationNeeded(response);
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
     * 진행 상태 메시지 → requestId로 userId 조회 후 Redis 이벤트 발행
     */
    private void handleProgress(OutfitKafkaResponse response) {
        TpoRequest tpoRequest =
                tpoRequestRepository.findByRequestId(response.requestId()).orElse(null);

        if (tpoRequest == null) {
            log.debug("[OutfitConsumer] progress: requestId 미존재, 무시 - {}", response.requestId());
            return;
        }

        Long userId = tpoRequest.getUser().getId();
        String sessionId =
                tpoRequest.getTpoSession() != null ? tpoRequest.getTpoSession().getSessionId() : null;

        OutfitWebSocketMessage message =
                OutfitWebSocketMessage.progress(response.requestId(), sessionId, response.step(), response.stepLabel());

        publishToUser(userId, message);
    }

    /**
     * 성공 응답 → DB 저장 + inflight 해제 + Redis 이벤트 발행 (outfit 데이터 포함)
     */
    private void handleSuccess(OutfitKafkaResponse response) {
        OutfitResultContext context = outfitResultService.handleSuccess(response);
        if (context == null) {
            return;
        }

        List<OutfitData> outfitData = toOutfitData(context.outfits());
        OutfitWebSocketMessage message = OutfitWebSocketMessage.success(
                response.requestId(), context.sessionId(), response.querySummary(), outfitData);
        publishToUser(context.userId(), message);
    }

    /**
     * 실패 응답 → DB 상태 변경 + inflight 해제 + Redis 이벤트 발행
     */
    private void handleFailure(OutfitKafkaResponse response) {
        OutfitResultContext context = outfitResultService.handleFailure(response);
        if (context == null) {
            return;
        }

        String errorCode = response.error() != null ? response.error().code() : "unknown";
        String errorMessage = response.error() != null ? response.error().message() : "알 수 없는 오류";

        OutfitWebSocketMessage message =
                OutfitWebSocketMessage.failed(response.requestId(), context.sessionId(), errorCode, errorMessage);

        publishToUser(context.userId(), message);
    }

    /**
     * 재질문 응답 → 상태 변경 + inflight 해제 + Redis 이벤트 발행
     */
    private void handleClarificationNeeded(OutfitKafkaResponse response) {
        OutfitResultContext context = outfitResultService.handleClarificationNeeded(response);
        if (context == null) {
            return;
        }

        OutfitWebSocketMessage message = OutfitWebSocketMessage.clarificationNeeded(
                response.requestId(), context.sessionId(), response.message());

        publishToUser(context.userId(), message);
    }

    private List<OutfitData> toOutfitData(List<OutfitSummary> summaries) {
        if (summaries == null || summaries.isEmpty()) {
            return List.of();
        }
        return summaries.stream()
                .map(s -> OutfitData.builder()
                        .resultId(s.resultId())
                        .clothesIds(s.clothesIds())
                        .reaction(s.reaction())
                        .vtonImageUrl(s.vtonImageUrl())
                        .build())
                .toList();
    }

    private void publishToUser(Long userId, OutfitWebSocketMessage message) {
        String channel = String.format(OUTFIT_CHANNEL, userId);
        redisEventPublisher.publish(channel, message);
    }
}
