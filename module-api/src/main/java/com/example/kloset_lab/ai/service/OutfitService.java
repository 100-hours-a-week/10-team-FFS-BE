package com.example.kloset_lab.ai.service;

import com.example.kloset_lab.ai.dto.OutfitAcceptedResponse;
import com.example.kloset_lab.ai.dto.TpoOutfitsRequest;
import com.example.kloset_lab.ai.entity.TpoRequest;
import com.example.kloset_lab.ai.entity.TpoSession;
import com.example.kloset_lab.ai.infrastructure.kafka.dto.OutfitKafkaRequest;
import com.example.kloset_lab.ai.infrastructure.kafka.producer.OutfitRequestProducer;
import com.example.kloset_lab.ai.repository.TpoRequestRepository;
import com.example.kloset_lab.ai.repository.TpoSessionRepository;
import com.example.kloset_lab.global.exception.CustomException;
import com.example.kloset_lab.global.exception.ErrorCode;
import com.example.kloset_lab.user.entity.User;
import com.example.kloset_lab.user.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutfitService {

    private final UserRepository userRepository;
    private final TpoSessionRepository tpoSessionRepository;
    private final TpoRequestRepository tpoRequestRepository;
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
