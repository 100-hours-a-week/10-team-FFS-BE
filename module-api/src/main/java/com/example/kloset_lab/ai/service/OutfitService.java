package com.example.kloset_lab.ai.service;

import com.example.kloset_lab.ai.dto.OutfitAcceptedResponse;
import com.example.kloset_lab.ai.dto.OutfitStatusResponse;
import com.example.kloset_lab.ai.dto.TpoFeedbackRequest;
import com.example.kloset_lab.ai.dto.TpoOutfitsRequest;
import com.example.kloset_lab.ai.entity.TpoOutbox;
import com.example.kloset_lab.ai.entity.TpoRequest;
import com.example.kloset_lab.ai.entity.TpoResult;
import com.example.kloset_lab.ai.entity.TpoSession;
import com.example.kloset_lab.ai.infrastructure.kafka.dto.OutfitKafkaRequest;
import com.example.kloset_lab.ai.infrastructure.kafka.dto.UploadSlot;
import com.example.kloset_lab.ai.repository.TpoOutboxRepository;
import com.example.kloset_lab.ai.repository.TpoRequestRepository;
import com.example.kloset_lab.ai.repository.TpoResultRepository;
import com.example.kloset_lab.ai.repository.TpoSessionRepository;
import com.example.kloset_lab.global.exception.CustomException;
import com.example.kloset_lab.global.exception.ErrorCode;
import com.example.kloset_lab.global.infrastructure.OutfitWebSocketMessage;
import com.example.kloset_lab.global.infrastructure.RedisEventPublisher;
import com.example.kloset_lab.media.dto.FileUploadInfo;
import com.example.kloset_lab.media.dto.FileUploadResponse;
import com.example.kloset_lab.media.entity.Purpose;
import com.example.kloset_lab.media.service.MediaService;
import com.example.kloset_lab.user.entity.User;
import com.example.kloset_lab.user.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutfitService {

    private static final String OUTFIT_CHANNEL = "outfit:event:%d";

    private final UserRepository userRepository;
    private final TpoSessionRepository tpoSessionRepository;
    private final TpoRequestRepository tpoRequestRepository;
    private final TpoResultRepository tpoResultRepository;
    private final TpoOutboxRepository tpoOutboxRepository;
    private final RedisEventPublisher redisEventPublisher;
    private final MediaService mediaService;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    private static final int VTON_UPLOAD_SLOT_COUNT = 3;

    /**
     * 코디 추천 요청 수락 (TX1 — Outbox 패턴)
     *
     * <p>TX1에서 TpoRequest + TpoOutbox를 원자적으로 저장한다.
     * Kafka 발행은 OutboxRelay가 PENDING 레코드를 감지해 처리하므로,
     * TX1 직후 프로세스가 종료되어도 재시작 후 자동 재발행된다.
     *
     * @param userId 사용자 ID
     * @param request 코디 추천 요청 DTO
     * @param sessionId 세션 ID (null이면 새 세션 생성)
     * @return 수락 응답 (requestId, sessionId, turnNo)
     */
    public OutfitAcceptedResponse requestOutfit(Long userId, TpoOutfitsRequest request, String sessionId) {

        // VTON presigned URL 발급 (S3 외부 호출 — TX 시작 전 수행)
        List<UploadSlot> uploadSlots = generateVtonUploadSlots(userId);

        // TX1: TpoRequest + TpoOutbox 원자적 저장
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

            // Outbox 레코드 저장 (Relay가 Kafka 발행 보장)
            String resolvedSessionId = session.getSessionId();
            OutfitKafkaRequest kafkaRequest =
                    OutfitKafkaRequest.of(requestId, userId, request.content(), resolvedSessionId, uploadSlots);
            try {
                tpoOutboxRepository.save(
                        TpoOutbox.pending(requestId, resolvedSessionId, objectMapper.writeValueAsString(kafkaRequest)));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("outbox payload 직렬화 실패", e);
            }

            return OutfitAcceptedResponse.of(requestId, resolvedSessionId, turnNo);
        });

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
     * 코디 추천 요청 취소 (TX + Redis 알림)
     *
     * <p>FE 타임아웃 감지 시 호출. PENDING 요청을 CANCELLED로 전환하고 inflight를 해제한다.
     * Kafka에 발행된 outfit-request 메시지는 정리하지 않으며, AI가 나중에 응답하더라도
     * isTerminal() 체크로 무해하게 스킵된다.
     *
     * @param userId 사용자 ID
     * @param requestId 취소할 요청 ID
     */
    public void cancelRequest(Long userId, String requestId) {
        String[] sessionIdHolder = new String[1];

        transactionTemplate.execute(status -> {
            TpoRequest tpoRequest = tpoRequestRepository
                    .findByRequestId(requestId)
                    .orElseThrow(() -> new CustomException(ErrorCode.RESULT_NOT_FOUND));

            if (!tpoRequest.getUser().getId().equals(userId)) {
                throw new CustomException(ErrorCode.TPO_RESULT_ACCESS_DENIED);
            }

            if (tpoRequest.isTerminal()) {
                throw new CustomException(ErrorCode.REQUEST_ALREADY_TERMINAL);
            }

            tpoRequest.cancel();

            TpoSession session = tpoRequest.getTpoSession();
            if (session != null) {
                TpoSession lockedSession = tpoSessionRepository
                        .findBySessionIdForUpdate(session.getSessionId())
                        .orElse(null);
                if (lockedSession != null) {
                    lockedSession.clearInflight(requestId);
                }
                sessionIdHolder[0] = session.getSessionId();
            }

            return null;
        });

        // TX 커밋 후 Redis 알림 발행
        OutfitWebSocketMessage message = OutfitWebSocketMessage.cancelled(requestId, sessionIdHolder[0]);
        redisEventPublisher.publish(String.format(OUTFIT_CHANNEL, userId), message);

        log.info("[OutfitService] 요청 취소 완료 - requestId: {}, userId: {}", requestId, userId);
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

    /**
     * VTON 이미지 업로드용 presigned URL 슬롯을 생성한다.
     *
     * @param userId 사용자 ID
     * @return UploadSlot 목록 (presignedUrl + fileId)
     */
    private List<UploadSlot> generateVtonUploadSlots(Long userId) {
        List<FileUploadInfo> fileInfos = IntStream.range(0, VTON_UPLOAD_SLOT_COUNT)
                .mapToObj(i -> FileUploadInfo.builder()
                        .name("vton_" + i + ".png")
                        .type("image/png")
                        .build())
                .toList();

        List<FileUploadResponse> responses = mediaService.requestFileUpload(userId, Purpose.VTON, fileInfos);

        return responses.stream()
                .map(r -> new UploadSlot(r.presignedUrl(), r.fileId()))
                .toList();
    }
}
