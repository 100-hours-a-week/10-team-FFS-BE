package com.example.kloset_lab.ai.infrastructure.kafka.consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.example.kloset_lab.ai.dto.OutfitResultContext;
import com.example.kloset_lab.ai.fixture.OutfitFixture;
import com.example.kloset_lab.ai.infrastructure.kafka.dto.OutfitKafkaResponse;
import com.example.kloset_lab.ai.service.OutfitResultService;
import com.example.kloset_lab.global.annotation.ServiceTest;
import com.example.kloset_lab.global.infrastructure.RedisEventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.kafka.support.Acknowledgment;

@ServiceTest
@DisplayName("OutfitResponseConsumer 단위 테스트")
class OutfitResponseConsumerTest {

    @Mock
    private OutfitResultService outfitResultService;

    @Mock
    private RedisEventPublisher redisEventPublisher;

    @Mock
    private Acknowledgment acknowledgment;

    private OutfitResponseConsumer consumer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        consumer = new OutfitResponseConsumer(outfitResultService, redisEventPublisher, objectMapper);
    }

    @Nested
    @DisplayName("processing 메시지")
    class ProcessingMessage {

        @Test
        @DisplayName("progress 메시지 수신 시 Redis 이벤트 발행 + acknowledge")
        void progress_정상_처리() throws Exception {
            OutfitResultContext context = OutfitResultContext.builder()
                    .userId(OutfitFixture.USER_ID)
                    .sessionId(OutfitFixture.SESSION_ID)
                    .build();
            OutfitKafkaResponse response = OutfitFixture.progressResponse();

            given(outfitResultService.handleProgress(any())).willReturn(context);

            ConsumerRecord<String, String> record =
                    new ConsumerRecord<>("outfit-response", 0, 0, null, objectMapper.writeValueAsString(response));

            consumer.consume(record, acknowledgment);

            then(outfitResultService).should().handleProgress(any());
            then(redisEventPublisher).should().publish(eq("outfit:event:1"), any());
            then(acknowledgment).should().acknowledge();
        }

        @Test
        @DisplayName("이미 처리된 요청의 progress 수신 시 Redis 발행 안 함 (중복 메시지 방어)")
        void progress_터미널_요청_미발행() throws Exception {
            given(outfitResultService.handleProgress(any())).willReturn(null);

            ConsumerRecord<String, String> record = new ConsumerRecord<>(
                    "outfit-response", 0, 0, null, objectMapper.writeValueAsString(OutfitFixture.progressResponse()));

            consumer.consume(record, acknowledgment);

            then(redisEventPublisher).should(never()).publish(anyString(), any());
            then(acknowledgment).should().acknowledge();
        }
    }

    @Nested
    @DisplayName("success 메시지")
    class SuccessMessage {

        @Test
        @DisplayName("성공 메시지 수신 시 서비스 호출 + Redis 이벤트 발행 + acknowledge")
        void success_정상_처리() throws Exception {
            OutfitResultContext context = OutfitResultContext.builder()
                    .userId(OutfitFixture.USER_ID)
                    .sessionId(OutfitFixture.SESSION_ID)
                    .build();
            given(outfitResultService.handleSuccess(any())).willReturn(context);

            ConsumerRecord<String, String> record = new ConsumerRecord<>(
                    "outfit-response", 0, 0, null, objectMapper.writeValueAsString(OutfitFixture.successResponse()));

            consumer.consume(record, acknowledgment);

            then(outfitResultService).should().handleSuccess(any());
            then(redisEventPublisher).should().publish(eq("outfit:event:1"), any());
            then(acknowledgment).should().acknowledge();
        }

        @Test
        @DisplayName("서비스가 null 반환 시 Redis 발행 안 함")
        void success_null_반환_시_Redis_미발행() throws Exception {
            given(outfitResultService.handleSuccess(any())).willReturn(null);

            ConsumerRecord<String, String> record = new ConsumerRecord<>(
                    "outfit-response", 0, 0, null, objectMapper.writeValueAsString(OutfitFixture.successResponse()));

            consumer.consume(record, acknowledgment);

            then(redisEventPublisher).should(never()).publish(anyString(), any());
            then(acknowledgment).should().acknowledge();
        }
    }

    @Nested
    @DisplayName("failed 메시지")
    class FailedMessage {

        @Test
        @DisplayName("실패 메시지 수신 시 서비스 호출 + Redis 이벤트 발행")
        void failed_정상_처리() throws Exception {
            OutfitResultContext context = OutfitResultContext.builder()
                    .userId(OutfitFixture.USER_ID)
                    .sessionId(OutfitFixture.SESSION_ID)
                    .build();
            given(outfitResultService.handleFailure(any())).willReturn(context);

            ConsumerRecord<String, String> record = new ConsumerRecord<>(
                    "outfit-response", 0, 0, null, objectMapper.writeValueAsString(OutfitFixture.failedResponse()));

            consumer.consume(record, acknowledgment);

            then(outfitResultService).should().handleFailure(any());
            then(redisEventPublisher).should().publish(eq("outfit:event:1"), any());
            then(acknowledgment).should().acknowledge();
        }
    }

    @Nested
    @DisplayName("clarification_needed 메시지")
    class ClarificationNeededMessage {

        @Test
        @DisplayName("재질문 메시지 수신 시 서비스 호출 + Redis 이벤트 발행 + acknowledge")
        void clarification_정상_처리() throws Exception {
            OutfitResultContext context = OutfitResultContext.builder()
                    .userId(OutfitFixture.USER_ID)
                    .sessionId(OutfitFixture.SESSION_ID)
                    .build();
            given(outfitResultService.handleClarificationNeeded(any())).willReturn(context);

            ConsumerRecord<String, String> record = new ConsumerRecord<>(
                    "outfit-response",
                    0,
                    0,
                    null,
                    objectMapper.writeValueAsString(OutfitFixture.clarificationNeededResponse()));

            consumer.consume(record, acknowledgment);

            then(outfitResultService).should().handleClarificationNeeded(any());
            then(redisEventPublisher).should().publish(eq("outfit:event:1"), any());
            then(acknowledgment).should().acknowledge();
        }
    }

    @Nested
    @DisplayName("예외 처리")
    class ExceptionHandling {

        @Test
        @DisplayName("JSON 파싱 실패 시 acknowledge 후 건너뜀")
        void json_파싱_실패() {
            ConsumerRecord<String, String> record =
                    new ConsumerRecord<>("outfit-response", 0, 0, null, "invalid json {{{");

            consumer.consume(record, acknowledgment);

            then(outfitResultService).should(never()).handleSuccess(any());
            then(outfitResultService).should(never()).handleFailure(any());
            then(acknowledgment).should().acknowledge();
        }

        @Test
        @DisplayName("서비스 예외 발생 시 acknowledge 안 함 (재시도)")
        void 서비스_예외_시_acknowledge_안함() throws Exception {
            given(outfitResultService.handleSuccess(any())).willThrow(new RuntimeException("DB error"));

            ConsumerRecord<String, String> record = new ConsumerRecord<>(
                    "outfit-response", 0, 0, null, objectMapper.writeValueAsString(OutfitFixture.successResponse()));

            consumer.consume(record, acknowledgment);

            then(acknowledgment).should(never()).acknowledge();
        }
    }
}
