package com.example.kloset_lab.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.example.kloset_lab.ai.dto.OutfitResultContext;
import com.example.kloset_lab.ai.entity.TpoRequest;
import com.example.kloset_lab.ai.entity.TpoResult;
import com.example.kloset_lab.ai.entity.TpoSession;
import com.example.kloset_lab.ai.fixture.OutfitFixture;
import com.example.kloset_lab.ai.infrastructure.kafka.dto.OutfitKafkaResponse;
import com.example.kloset_lab.ai.repository.TpoRequestRepository;
import com.example.kloset_lab.ai.repository.TpoResultClothesRepository;
import com.example.kloset_lab.ai.repository.TpoResultRepository;
import com.example.kloset_lab.ai.repository.TpoSessionRepository;
import com.example.kloset_lab.clothes.repository.ClothesRepository;
import com.example.kloset_lab.global.annotation.ServiceTest;
import com.example.kloset_lab.user.entity.User;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@ServiceTest
@DisplayName("OutfitResultService 단위 테스트 (TX3 응답 처리)")
class OutfitResultServiceTest {

    @Mock
    private TpoRequestRepository tpoRequestRepository;

    @Mock
    private TpoSessionRepository tpoSessionRepository;

    @Mock
    private TpoResultRepository tpoResultRepository;

    @Mock
    private TpoResultClothesRepository tpoResultClothesRepository;

    @Mock
    private ClothesRepository clothesRepository;

    @InjectMocks
    private OutfitResultService outfitResultService;

    private User user;
    private TpoSession session;

    @BeforeEach
    void setUp() {
        user = OutfitFixture.testUser(OutfitFixture.USER_ID);
        session = OutfitFixture.testSession(user);
    }

    @Nested
    @DisplayName("handleSuccess (성공 응답 처리)")
    class HandleSuccess {

        @Test
        @DisplayName("정상 처리: 결과 저장 + inflight 해제 + 컨텍스트 반환")
        void 정상_성공_처리() {
            TpoRequest tpoRequest = OutfitFixture.testRequest(user, session);
            session.startInflight(OutfitFixture.REQUEST_ID);
            OutfitKafkaResponse response = OutfitFixture.successResponse();

            given(tpoRequestRepository.findByRequestId(OutfitFixture.REQUEST_ID))
                    .willReturn(Optional.of(tpoRequest));
            given(tpoResultRepository.save(any(TpoResult.class))).willAnswer(i -> i.getArgument(0));
            given(clothesRepository.findAllById(any())).willReturn(List.of());
            given(tpoSessionRepository.findBySessionIdForUpdate(OutfitFixture.SESSION_ID))
                    .willReturn(Optional.of(session));

            OutfitResultContext context = outfitResultService.handleSuccess(response);

            assertThat(context).isNotNull();
            assertThat(context.userId()).isEqualTo(OutfitFixture.USER_ID);
            assertThat(context.sessionId()).isEqualTo(OutfitFixture.SESSION_ID);
            assertThat(tpoRequest.isCompleted()).isTrue();
            assertThat(session.isInflight()).isFalse();
        }

        @Test
        @DisplayName("requestId 미존재 시 null 반환")
        void requestId_미존재() {
            given(tpoRequestRepository.findByRequestId(OutfitFixture.REQUEST_ID))
                    .willReturn(Optional.empty());

            OutfitResultContext context = outfitResultService.handleSuccess(OutfitFixture.successResponse());

            assertThat(context).isNull();
            then(tpoResultRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("이미 처리된 요청은 건너뜀 (멱등성)")
        void 이미_완료된_요청() {
            TpoRequest completed = OutfitFixture.completedRequest(user, session);

            given(tpoRequestRepository.findByRequestId(OutfitFixture.REQUEST_ID))
                    .willReturn(Optional.of(completed));

            OutfitResultContext context = outfitResultService.handleSuccess(OutfitFixture.successResponse());

            assertThat(context).isNull();
            then(tpoResultRepository).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("handleFailure (실패 응답 처리)")
    class HandleFailure {

        @Test
        @DisplayName("정상 처리: FAILED 상태 변경 + inflight 해제")
        void 정상_실패_처리() {
            TpoRequest tpoRequest = OutfitFixture.testRequest(user, session);
            session.startInflight(OutfitFixture.REQUEST_ID);

            given(tpoRequestRepository.findByRequestId(OutfitFixture.REQUEST_ID))
                    .willReturn(Optional.of(tpoRequest));
            given(tpoSessionRepository.findBySessionIdForUpdate(OutfitFixture.SESSION_ID))
                    .willReturn(Optional.of(session));

            OutfitResultContext context = outfitResultService.handleFailure(OutfitFixture.failedResponse());

            assertThat(context).isNotNull();
            assertThat(context.userId()).isEqualTo(OutfitFixture.USER_ID);
            assertThat(tpoRequest.getStatus().name()).isEqualTo("FAILED");
            assertThat(session.isInflight()).isFalse();
        }

        @Test
        @DisplayName("requestId 미존재 시 null 반환")
        void requestId_미존재() {
            given(tpoRequestRepository.findByRequestId(OutfitFixture.REQUEST_ID))
                    .willReturn(Optional.empty());

            OutfitResultContext context = outfitResultService.handleFailure(OutfitFixture.failedResponse());

            assertThat(context).isNull();
        }

        @Test
        @DisplayName("이미 처리된 요청은 건너뜀 (멱등성)")
        void 이미_완료된_요청() {
            TpoRequest completed = OutfitFixture.completedRequest(user, session);

            given(tpoRequestRepository.findByRequestId(OutfitFixture.REQUEST_ID))
                    .willReturn(Optional.of(completed));

            OutfitResultContext context = outfitResultService.handleFailure(OutfitFixture.failedResponse());

            assertThat(context).isNull();
        }
    }

    @Nested
    @DisplayName("handleClarificationNeeded (재질문 응답 처리)")
    class HandleClarificationNeeded {

        @Test
        @DisplayName("정상 처리: CLARIFICATION_NEEDED 상태 변경 + inflight 해제")
        void 정상_재질문_처리() {
            TpoRequest tpoRequest = OutfitFixture.testRequest(user, session);
            session.startInflight(OutfitFixture.REQUEST_ID);

            given(tpoRequestRepository.findByRequestId(OutfitFixture.REQUEST_ID))
                    .willReturn(Optional.of(tpoRequest));
            given(tpoSessionRepository.findBySessionIdForUpdate(OutfitFixture.SESSION_ID))
                    .willReturn(Optional.of(session));

            OutfitResultContext context =
                    outfitResultService.handleClarificationNeeded(OutfitFixture.clarificationNeededResponse());

            assertThat(context).isNotNull();
            assertThat(context.userId()).isEqualTo(OutfitFixture.USER_ID);
            assertThat(tpoRequest.getStatus().name()).isEqualTo("CLARIFICATION_NEEDED");
            assertThat(session.isInflight()).isFalse();
            then(tpoResultRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("requestId 미존재 시 null 반환")
        void requestId_미존재() {
            given(tpoRequestRepository.findByRequestId(OutfitFixture.REQUEST_ID))
                    .willReturn(Optional.empty());

            OutfitResultContext context =
                    outfitResultService.handleClarificationNeeded(OutfitFixture.clarificationNeededResponse());

            assertThat(context).isNull();
        }

        @Test
        @DisplayName("이미 처리된 요청은 건너뜀 (멱등성)")
        void 이미_완료된_요청() {
            TpoRequest completed = OutfitFixture.completedRequest(user, session);

            given(tpoRequestRepository.findByRequestId(OutfitFixture.REQUEST_ID))
                    .willReturn(Optional.of(completed));

            OutfitResultContext context =
                    outfitResultService.handleClarificationNeeded(OutfitFixture.clarificationNeededResponse());

            assertThat(context).isNull();
        }
    }
}
