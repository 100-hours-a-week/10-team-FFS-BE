package com.example.kloset_lab.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.example.kloset_lab.ai.dto.OutfitAcceptedResponse;
import com.example.kloset_lab.ai.dto.OutfitStatusResponse;
import com.example.kloset_lab.ai.dto.TpoFeedbackRequest;
import com.example.kloset_lab.ai.dto.TpoOutfitsRequest;
import com.example.kloset_lab.ai.entity.Reaction;
import com.example.kloset_lab.ai.entity.TpoRequest;
import com.example.kloset_lab.ai.entity.TpoResult;
import com.example.kloset_lab.ai.entity.TpoSession;
import com.example.kloset_lab.ai.fixture.OutfitFixture;
import com.example.kloset_lab.ai.infrastructure.kafka.dto.OutfitKafkaRequest;
import com.example.kloset_lab.ai.infrastructure.kafka.producer.OutfitRequestProducer;
import com.example.kloset_lab.ai.repository.TpoRequestRepository;
import com.example.kloset_lab.ai.repository.TpoResultRepository;
import com.example.kloset_lab.ai.repository.TpoSessionRepository;
import com.example.kloset_lab.global.exception.CustomException;
import com.example.kloset_lab.global.exception.ErrorCode;
import com.example.kloset_lab.user.entity.User;
import com.example.kloset_lab.user.repository.UserRepository;
import java.util.Optional;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@com.example.kloset_lab.global.annotation.ServiceTest
@DisplayName("OutfitService 단위 테스트")
class OutfitServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TpoSessionRepository tpoSessionRepository;

    @Mock
    private TpoRequestRepository tpoRequestRepository;

    @Mock
    private TpoResultRepository tpoResultRepository;

    @Mock
    private OutfitRequestProducer outfitRequestProducer;

    @Mock
    private TransactionTemplate transactionTemplate;

    private OutfitService outfitService;

    private User user;
    private TpoSession session;

    @BeforeEach
    void setUp() {
        outfitService = new OutfitService(
                userRepository,
                tpoSessionRepository,
                tpoRequestRepository,
                tpoResultRepository,
                outfitRequestProducer,
                transactionTemplate);

        user = OutfitFixture.testUser(OutfitFixture.USER_ID);
        session = OutfitFixture.testSession(user);
    }

    @Nested
    @DisplayName("requestOutfit (TX1 코디 추천 요청)")
    class RequestOutfit {

        private final TpoOutfitsRequest request = new TpoOutfitsRequest(OutfitFixture.CONTENT);

        @Test
        @DisplayName("새 세션으로 정상 요청 시 202 Accepted 반환 및 Kafka 발행")
        void 새_세션_정상_요청() {
            // TransactionTemplate.execute를 실제로 실행하도록 stub
            given(transactionTemplate.execute(any())).willAnswer(invocation -> {
                TransactionCallback<?> callback = invocation.getArgument(0);
                return callback.doInTransaction(null);
            });
            given(userRepository.findById(OutfitFixture.USER_ID)).willReturn(Optional.of(user));
            given(tpoSessionRepository.save(any(TpoSession.class))).willReturn(session);
            given(tpoRequestRepository.save(any(TpoRequest.class))).willAnswer(i -> i.getArgument(0));

            OutfitAcceptedResponse response = outfitService.requestOutfit(OutfitFixture.USER_ID, request, null);

            assertThat(response).isNotNull();
            assertThat(response.status()).isEqualTo("accepted");
            assertThat(response.turnNo()).isEqualTo(1);

            // Kafka 발행 검증
            ArgumentCaptor<OutfitKafkaRequest> captor = ArgumentCaptor.forClass(OutfitKafkaRequest.class);
            then(outfitRequestProducer).should().send(captor.capture());
            assertThat(captor.getValue().userId()).isEqualTo(OutfitFixture.USER_ID);
        }

        @Test
        @DisplayName("기존 세션으로 요청 시 turnNo 증가")
        void 기존_세션_요청() {
            given(transactionTemplate.execute(any())).willAnswer(invocation -> {
                TransactionCallback<?> callback = invocation.getArgument(0);
                return callback.doInTransaction(null);
            });
            given(userRepository.findById(OutfitFixture.USER_ID)).willReturn(Optional.of(user));
            given(tpoSessionRepository.findBySessionIdForUpdate(OutfitFixture.SESSION_ID))
                    .willReturn(Optional.of(session));
            given(tpoRequestRepository.save(any(TpoRequest.class))).willAnswer(i -> i.getArgument(0));

            OutfitAcceptedResponse response =
                    outfitService.requestOutfit(OutfitFixture.USER_ID, request, OutfitFixture.SESSION_ID);

            assertThat(response.sessionId()).isEqualTo(OutfitFixture.SESSION_ID);
            assertThat(response.turnNo()).isEqualTo(1);
        }

        @Test
        @DisplayName("세션이 inflight 상태이면 SESSION_BUSY")
        void 세션_inflight_시_SESSION_BUSY() {
            TpoSession inflightSession = OutfitFixture.inflightSession(user);

            given(transactionTemplate.execute(any())).willAnswer(invocation -> {
                TransactionCallback<?> callback = invocation.getArgument(0);
                return callback.doInTransaction(null);
            });
            given(userRepository.findById(OutfitFixture.USER_ID)).willReturn(Optional.of(user));
            given(tpoSessionRepository.findBySessionIdForUpdate(OutfitFixture.SESSION_ID))
                    .willReturn(Optional.of(inflightSession));

            assertCustomException(
                    () -> outfitService.requestOutfit(OutfitFixture.USER_ID, request, OutfitFixture.SESSION_ID),
                    ErrorCode.SESSION_BUSY);

            then(outfitRequestProducer).should(never()).send(any());
        }

        @Test
        @DisplayName("사용자가 존재하지 않으면 USER_NOT_FOUND")
        void 사용자_미존재() {
            given(transactionTemplate.execute(any())).willAnswer(invocation -> {
                TransactionCallback<?> callback = invocation.getArgument(0);
                return callback.doInTransaction(null);
            });
            given(userRepository.findById(OutfitFixture.USER_ID)).willReturn(Optional.empty());

            assertCustomException(
                    () -> outfitService.requestOutfit(OutfitFixture.USER_ID, request, null), ErrorCode.USER_NOT_FOUND);
        }

        @Test
        @DisplayName("다른 사용자의 세션이면 ACCESS_DENIED")
        void 타인_세션_접근() {
            User otherUser = OutfitFixture.testUser(OutfitFixture.OTHER_USER_ID);
            TpoSession otherSession = OutfitFixture.testSession(otherUser);

            given(transactionTemplate.execute(any())).willAnswer(invocation -> {
                TransactionCallback<?> callback = invocation.getArgument(0);
                return callback.doInTransaction(null);
            });
            given(userRepository.findById(OutfitFixture.USER_ID)).willReturn(Optional.of(user));
            given(tpoSessionRepository.findBySessionIdForUpdate(OutfitFixture.SESSION_ID))
                    .willReturn(Optional.of(otherSession));

            assertCustomException(
                    () -> outfitService.requestOutfit(OutfitFixture.USER_ID, request, OutfitFixture.SESSION_ID),
                    ErrorCode.ACCESS_DENIED);
        }
    }

    @Nested
    @DisplayName("recordReaction (TX2 피드백 등록)")
    class RecordReaction {

        @Test
        @DisplayName("정상 피드백 등록")
        void 정상_피드백() {
            TpoRequest tpoRequest = OutfitFixture.testRequest(user, session);
            TpoResult tpoResult = OutfitFixture.testResult(tpoRequest);
            // 세션의 lastTurnNo를 요청의 turnNo와 일치시킴
            session.nextTurn();

            given(tpoResultRepository.findByIdWithSession(100L)).willReturn(Optional.of(tpoResult));
            given(tpoSessionRepository.findBySessionIdForUpdate(OutfitFixture.SESSION_ID))
                    .willReturn(Optional.of(session));

            outfitService.recordReaction(OutfitFixture.USER_ID, 100L, new TpoFeedbackRequest(Reaction.GOOD));

            assertThat(tpoResult.getReaction()).isEqualTo(Reaction.GOOD);
        }
        @Test
        @DisplayName("최신 턴이 아니면 NOT_LATEST_TURN_RESULT")
        void 최신_턴_아닌_피드백() {
            TpoRequest tpoRequest = OutfitFixture.testRequest(user, session);
            TpoResult tpoResult = OutfitFixture.testResult(tpoRequest);
            // 세션 lastTurnNo를 2로 설정하여 요청의 turnNo(1)과 불일치
            session.nextTurn();
            session.nextTurn();

            given(tpoResultRepository.findByIdWithSession(100L)).willReturn(Optional.of(tpoResult));
            given(tpoSessionRepository.findBySessionIdForUpdate(OutfitFixture.SESSION_ID))
                    .willReturn(Optional.of(session));

            assertCustomException(
                    () -> outfitService.recordReaction(
                            OutfitFixture.USER_ID, 100L, new TpoFeedbackRequest(Reaction.GOOD)),
                    ErrorCode.NOT_LATEST_TURN_RESULT);
        }

        @Test
        @DisplayName("세션 inflight 중 피드백 시 SESSION_INFLIGHT")
        void 세션_inflight_중_피드백() {
            TpoRequest tpoRequest = OutfitFixture.testRequest(user, session);
            TpoResult tpoResult = OutfitFixture.testResult(tpoRequest);
            TpoSession inflightSession = OutfitFixture.inflightSession(user);
            // lastTurnNo를 일치시킴
            inflightSession.nextTurn();

            given(tpoResultRepository.findByIdWithSession(100L)).willReturn(Optional.of(tpoResult));
            given(tpoSessionRepository.findBySessionIdForUpdate(OutfitFixture.SESSION_ID))
                    .willReturn(Optional.of(inflightSession));

            assertCustomException(
                    () -> outfitService.recordReaction(
                            OutfitFixture.USER_ID, 100L, new TpoFeedbackRequest(Reaction.GOOD)),
                    ErrorCode.SESSION_INFLIGHT);
        }

        @Test
        @DisplayName("타인의 결과에 피드백 시 TPO_RESULT_ACCESS_DENIED")
        void 타인_결과_피드백() {
            TpoRequest tpoRequest = OutfitFixture.testRequest(user, session);
            TpoResult tpoResult = OutfitFixture.testResult(tpoRequest);

            given(tpoResultRepository.findByIdWithSession(100L)).willReturn(Optional.of(tpoResult));

            assertCustomException(
                    () -> outfitService.recordReaction(
                            OutfitFixture.OTHER_USER_ID, 100L, new TpoFeedbackRequest(Reaction.GOOD)),
                    ErrorCode.TPO_RESULT_ACCESS_DENIED);
        }
    }

    @Nested
    @DisplayName("getRequestStatus (상태 복구 조회)")
    class GetRequestStatus {

        @Test
        @DisplayName("정상 상태 조회")
        void 정상_조회() {
            TpoRequest tpoRequest = OutfitFixture.testRequest(user, session);

            given(tpoRequestRepository.findByRequestId(OutfitFixture.REQUEST_ID))
                    .willReturn(Optional.of(tpoRequest));

            OutfitStatusResponse response =
                    outfitService.getRequestStatus(OutfitFixture.USER_ID, OutfitFixture.REQUEST_ID);

            assertThat(response.requestId()).isEqualTo(OutfitFixture.REQUEST_ID);
            assertThat(response.status()).isEqualTo("PENDING");
            assertThat(response.sessionId()).isEqualTo(OutfitFixture.SESSION_ID);
        }

        @Test
        @DisplayName("타인의 요청 조회 시 TPO_RESULT_ACCESS_DENIED")
        void 타인_요청_조회() {
            TpoRequest tpoRequest = OutfitFixture.testRequest(user, session);

            given(tpoRequestRepository.findByRequestId(OutfitFixture.REQUEST_ID))
                    .willReturn(Optional.of(tpoRequest));

            assertCustomException(
                    () -> outfitService.getRequestStatus(OutfitFixture.OTHER_USER_ID, OutfitFixture.REQUEST_ID),
                    ErrorCode.TPO_RESULT_ACCESS_DENIED);
        }
    }

    private void assertCustomException(ThrowingCallable callable, ErrorCode expectedCode) {
        assertThatThrownBy(callable)
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(expectedCode);
    }
}
