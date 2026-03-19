package com.example.kloset_lab.ai.service;

import com.example.kloset_lab.ai.dto.OutfitAcceptedResponse;
import com.example.kloset_lab.ai.dto.OutfitStatusResponse;
import com.example.kloset_lab.ai.dto.TpoFeedbackRequest;
import com.example.kloset_lab.ai.dto.TpoOutfitsRequest;
import com.example.kloset_lab.ai.entity.TpoRequest;
import com.example.kloset_lab.ai.entity.TpoResult;
import com.example.kloset_lab.ai.entity.TpoSession;
import com.example.kloset_lab.ai.infrastructure.kafka.dto.OutfitKafkaRequest;
import com.example.kloset_lab.ai.infrastructure.kafka.producer.OutfitRequestProducer;
import com.example.kloset_lab.ai.repository.TpoRequestRepository;
import com.example.kloset_lab.ai.repository.TpoResultRepository;
import com.example.kloset_lab.ai.repository.TpoSessionRepository;
import com.example.kloset_lab.global.exception.CustomException;
import com.example.kloset_lab.global.exception.ErrorCode;
import com.example.kloset_lab.user.entity.User;
import com.example.kloset_lab.user.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutfitService {

    private final UserRepository userRepository;
    private final TpoSessionRepository tpoSessionRepository;
    private final TpoRequestRepository tpoRequestRepository;
    private final TpoResultRepository tpoResultRepository;
    private final OutfitRequestProducer outfitRequestProducer;
    private final TransactionTemplate transactionTemplate;

    /**
     * 코디 추천 요청 수락 (TX1 + Kafka 발행)
     *
     * <p>TX1에서 세션 잠금, inflight 확인, TpoRequest 생성을 처리한 뒤
     * TX 커밋 후 Kafka에 메시지를 발행한다.
     *
     * @param userId 사용자 ID
     * @param request 코디 추천 요청 DTO
     * @param sessionId 세션 ID (null이면 새 세션 생성)
     * @return 수락 응답 (requestId, sessionId, turnNo)
     */
    public OutfitAcceptedResponse requestOutfit(Long userId, TpoOutfitsRequest request, String sessionId) {

        OutfitAcceptedResponse response = transactionTemplate.execute(status -> {
            User user =
                    userRepository.findById(userId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

            TpoSession session = resolveSession(user, sessionId);

            if (session.isInflight()) {
                throw new CustomException(ErrorCode.SESSION_BUSY);
            }

            String requestId = UUID.randomUUID().toString();
            int turnNo = session.nextTurn();

            TpoRequest tpoRequest = new TpoRequest(user, session, requestId, turnNo, request.content());
            tpoRequestRepository.save(tpoRequest);

            session.startInflight(requestId);

            return OutfitAcceptedResponse.of(requestId, session.getSessionId(), turnNo);
        });

        // TX 커밋 후 Kafka 발행
        outfitRequestProducer.send(
                OutfitKafkaRequest.of(response.requestId(), userId, request.content(), response.sessionId(), null));

        return response;
    }

    /**
     * TX3: 코디 결과 피드백 등록 (세션 기반 서버 규칙 적용)
     *
     * <p>규칙 1: 최신 턴 이외 리액션 금지
     * <p>규칙 2: 세션 in-flight 중 리액션 금지
     *
     * @param userId 사용자 ID
     * @param resultId 결과 ID
     * @param request 피드백 요청 DTO
     */
    @Transactional
    public void recordReaction(Long userId, Long resultId, TpoFeedbackRequest request) {
        TpoResult tpoResult = tpoResultRepository
                .findByIdWithSession(resultId)
                .orElseThrow(() -> new CustomException(ErrorCode.TPO_RESULT_NOT_FOUND));

        // 소유권 검증
        Long ownerId = tpoResult.getTpoRequest().getUser().getId();
        if (!ownerId.equals(userId)) {
            throw new CustomException(ErrorCode.TPO_RESULT_ACCESS_DENIED);
        }

        TpoSession session = tpoResult.getTpoRequest().getTpoSession();
        if (session != null) {
            // 세션 잠금
            TpoSession lockedSession = tpoSessionRepository
                    .findBySessionIdForUpdate(session.getSessionId())
                    .orElseThrow(() -> new CustomException(ErrorCode.SESSION_NOT_FOUND));

            // 규칙 1: 최신 턴 이외 리액션 금지
            if (!tpoResult.getTpoRequest().getTurnNo().equals(lockedSession.getLastTurnNo())) {
                throw new CustomException(ErrorCode.NOT_LATEST_TURN_RESULT);
            }

            // 규칙 2: 세션 in-flight 중 리액션 금지
            if (lockedSession.isInflight()) {
                throw new CustomException(ErrorCode.SESSION_INFLIGHT);
            }
        }

        tpoResult.updateReaction(request.reaction());
    }

    /**
     * 요청 상태 조회 (상태 복구용 — WebSocket 재연결 시 클라이언트 호출)
     *
     * @param userId 사용자 ID
     * @param requestId 요청 추적 ID
     * @return 요청 상태 응답
     */
    @Transactional(readOnly = true)
    public OutfitStatusResponse getRequestStatus(Long userId, String requestId) {
        TpoRequest tpoRequest = tpoRequestRepository
                .findByRequestId(requestId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESULT_NOT_FOUND));

        if (!tpoRequest.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.TPO_RESULT_ACCESS_DENIED);
        }

        TpoSession session = tpoRequest.getTpoSession();
        return OutfitStatusResponse.builder()
                .requestId(requestId)
                .sessionId(session != null ? session.getSessionId() : null)
                .turnNo(tpoRequest.getTurnNo())
                .status(tpoRequest.getStatus() != null ? tpoRequest.getStatus().name() : null)
                .build();
    }

    /**
     * 세션을 조회하거나 새로 생성한다.
     *
     * @param user 사용자
     * @param sessionId 세션 ID (null이면 새 세션 생성)
     * @return TpoSession (기존 세션은 FOR UPDATE 잠금)
     */
    private TpoSession resolveSession(User user, String sessionId) {
        if (sessionId == null) {
            return tpoSessionRepository.save(TpoSession.builder().user(user).build());
        }

        TpoSession session = tpoSessionRepository
                .findBySessionIdForUpdate(sessionId)
                .orElseThrow(() -> new CustomException(ErrorCode.SESSION_NOT_FOUND));

        if (!session.getUser().getId().equals(user.getId())) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        return session;
    }
}
