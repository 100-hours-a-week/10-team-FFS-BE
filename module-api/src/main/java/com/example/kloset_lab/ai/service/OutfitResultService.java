package com.example.kloset_lab.ai.service;

import com.example.kloset_lab.ai.dto.OutfitResultContext;
import com.example.kloset_lab.ai.entity.TpoRequest;
import com.example.kloset_lab.ai.entity.TpoResult;
import com.example.kloset_lab.ai.entity.TpoResultClothes;
import com.example.kloset_lab.ai.entity.TpoSession;
import com.example.kloset_lab.ai.infrastructure.kafka.dto.OutfitKafkaResponse;
import com.example.kloset_lab.ai.repository.TpoRequestRepository;
import com.example.kloset_lab.ai.repository.TpoResultClothesRepository;
import com.example.kloset_lab.ai.repository.TpoResultRepository;
import com.example.kloset_lab.ai.repository.TpoSessionRepository;
import com.example.kloset_lab.clothes.entity.Clothes;
import com.example.kloset_lab.clothes.repository.ClothesRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * TX3: 코디추천 AI 응답 처리 (결과 저장 + inflight 해제)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutfitResultService {

    private final TpoRequestRepository tpoRequestRepository;
    private final TpoSessionRepository tpoSessionRepository;
    private final TpoResultRepository tpoResultRepository;
    private final TpoResultClothesRepository tpoResultClothesRepository;
    private final ClothesRepository clothesRepository;

    /**
     * 성공 응답 처리: 결과 저장 + inflight 해제
     *
     * @param response Kafka 성공 메시지
     * @return 처리 컨텍스트 (WebSocket 발행용), 무시된 경우 null
     */
    @Transactional
    public OutfitResultContext handleSuccess(OutfitKafkaResponse response) {
        TpoRequest tpoRequest =
                tpoRequestRepository.findByRequestId(response.requestId()).orElse(null);

        if (tpoRequest == null) {
            log.warn("[OutfitResult] requestId 미존재, 무시 - requestId: {}", response.requestId());
            return null;
        }

        // 멱등성: 이미 터미널 상태(COMPLETED/FAILED)인 요청은 건너뜀
        if (tpoRequest.isTerminal()) {
            log.info("[OutfitResult] 이미 처리된 요청, 건너뜀 - requestId: {}", response.requestId());
            return null;
        }

        // 결과 저장
        saveOutfitResults(tpoRequest, response);

        // 상태 변경
        tpoRequest.complete();

        // inflight 해제 (세션 잠금)
        clearInflight(tpoRequest);

        log.info(
                "[OutfitResult] 성공 처리 완료 - requestId: {}, outfits: {}",
                response.requestId(),
                response.outfits() != null ? response.outfits().size() : 0);

        return buildContext(tpoRequest);
    }

    /**
     * 실패 응답 처리: 상태 FAILED 변경 + inflight 해제
     *
     * @param response Kafka 실패 메시지
     * @return 처리 컨텍스트 (WebSocket 발행용), 무시된 경우 null
     */
    @Transactional
    public OutfitResultContext handleFailure(OutfitKafkaResponse response) {
        TpoRequest tpoRequest =
                tpoRequestRepository.findByRequestId(response.requestId()).orElse(null);

        if (tpoRequest == null) {
            log.warn("[OutfitResult] requestId 미존재, 무시 - requestId: {}", response.requestId());
            return null;
        }

        if (tpoRequest.isTerminal()) {
            log.info("[OutfitResult] 이미 처리된 요청, 건너뜀 - requestId: {}", response.requestId());
            return null;
        }

        tpoRequest.fail();
        clearInflight(tpoRequest);

        log.warn(
                "[OutfitResult] 실패 처리 완료 - requestId: {}, errorCode: {}, message: {}",
                response.requestId(),
                response.error() != null ? response.error().code() : "unknown",
                response.error() != null ? response.error().message() : "unknown");

        return buildContext(tpoRequest);
    }

    /**
     * 재질문 응답 처리: 상태 CLARIFICATION_NEEDED 변경 + inflight 해제
     *
     * <p>결과 저장 없이 inflight만 해제하여 사용자가 후속 요청을 보낼 수 있도록 한다.
     *
     * @param response Kafka clarification_needed 메시지
     * @return 처리 컨텍스트 (WebSocket 발행용), 무시된 경우 null
     */
    @Transactional
    public OutfitResultContext handleClarificationNeeded(OutfitKafkaResponse response) {
        TpoRequest tpoRequest =
                tpoRequestRepository.findByRequestId(response.requestId()).orElse(null);

        if (tpoRequest == null) {
            log.warn("[OutfitResult] requestId 미존재, 무시 - requestId: {}", response.requestId());
            return null;
        }

        if (tpoRequest.isTerminal()) {
            log.info("[OutfitResult] 이미 처리된 요청, 건너뜀 - requestId: {}", response.requestId());
            return null;
        }

        tpoRequest.clarificationNeeded();
        clearInflight(tpoRequest);

        log.info("[OutfitResult] 재질문 처리 완료 - requestId: {}, message: {}", response.requestId(), response.message());

        return buildContext(tpoRequest);
    }

    /**
     * 코디 결과(TpoResult + TpoResultClothes)를 저장한다.
     */
    private void saveOutfitResults(TpoRequest tpoRequest, OutfitKafkaResponse response) {
        if (response.outfits() == null || response.outfits().isEmpty()) {
            return;
        }

        for (OutfitKafkaResponse.Outfit outfit : response.outfits()) {
            TpoResult tpoResult = tpoResultRepository.save(TpoResult.builder()
                    .tpoRequest(tpoRequest)
                    .cordiExplainText("")
                    .outfitId(String.valueOf(tpoRequest.getId()) + "_"
                            + response.outfits().indexOf(outfit))
                    .vtonImageUrl(outfit.vtonImageUrl())
                    .build());

            if (outfit.items() != null) {
                List<Clothes> clothesList = clothesRepository.findAllById(outfit.items());

                tpoResultClothesRepository.saveAll(clothesList.stream()
                        .map(clothes -> TpoResultClothes.builder()
                                .tpoResult(tpoResult)
                                .clothes(clothes)
                                .build())
                        .toList());
            }
        }
    }

    private OutfitResultContext buildContext(TpoRequest tpoRequest) {
        TpoSession session = tpoRequest.getTpoSession();
        String sessionId = session != null ? session.getSessionId() : null;
        return new OutfitResultContext(tpoRequest.getUser().getId(), sessionId);
    }

    /**
     * 세션의 inflight를 해제한다. (FOR UPDATE 잠금)
     */
    private void clearInflight(TpoRequest tpoRequest) {
        TpoSession session = tpoRequest.getTpoSession();
        if (session == null) {
            return;
        }

        TpoSession lockedSession = tpoSessionRepository
                .findBySessionIdForUpdate(session.getSessionId())
                .orElse(null);

        if (lockedSession != null) {
            lockedSession.clearInflight(tpoRequest.getRequestId());
        }
    }
}
