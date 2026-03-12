package com.example.kloset_lab.ai.service;

import com.example.kloset_lab.ai.dto.SessionHistoryResponse;
import com.example.kloset_lab.ai.entity.TpoRequest;
import com.example.kloset_lab.ai.entity.TpoResult;
import com.example.kloset_lab.ai.repository.TpoRequestRepository;
import com.example.kloset_lab.ai.repository.TpoResultRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 세션 히스토리 조회 서비스 (Internal API용)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SessionHistoryService {

    private final TpoRequestRepository tpoRequestRepository;
    private final TpoResultRepository tpoResultRepository;

    /**
     * 세션 히스토리를 최신 턴부터 내림차순으로 조회한다.
     *
     * @param sessionId 세션 ID
     * @param userId 사용자 ID
     * @param uptoTurnNo 조회 상한 턴 번호
     * @param limit 최대 조회 건수
     * @return 세션 히스토리 응답
     */
    public SessionHistoryResponse getSessionHistory(String sessionId, Long userId, int uptoTurnNo, int limit) {
        List<TpoRequest> requests =
                tpoRequestRepository.findSessionHistory(sessionId, userId, uptoTurnNo, PageRequest.of(0, limit));

        List<SessionHistoryResponse.TurnHistory> turns =
                requests.stream().map(this::toTurnHistory).toList();

        return SessionHistoryResponse.builder()
                .sessionId(sessionId)
                .uptoTurnNo(uptoTurnNo)
                .turns(turns)
                .build();
    }

    private SessionHistoryResponse.TurnHistory toTurnHistory(TpoRequest request) {
        List<TpoResult> results = tpoResultRepository.findByTpoRequest(request);

        List<Long> outfitIds = results.stream().map(TpoResult::getId).toList();

        String reaction = results.stream()
                .map(r -> r.getReaction().name())
                .filter(name -> !"NONE".equals(name))
                .findFirst()
                .orElse("NONE");

        return SessionHistoryResponse.TurnHistory.builder()
                .turnNo(request.getTurnNo())
                .requestText(request.getRequestText())
                .querySummary(request.getQuerySummary())
                .outfitIds(outfitIds)
                .reaction(reaction)
                .createdAt(request.getCreatedAt())
                .build();
    }
}
