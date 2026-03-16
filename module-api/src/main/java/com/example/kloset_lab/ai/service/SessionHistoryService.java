package com.example.kloset_lab.ai.service;

import com.example.kloset_lab.ai.dto.SessionHistoryResponse;
import com.example.kloset_lab.ai.entity.TpoRequest;
import com.example.kloset_lab.ai.entity.TpoResult;
import com.example.kloset_lab.ai.repository.TpoRequestRepository;
import com.example.kloset_lab.ai.repository.TpoResultClothesRepository;
import com.example.kloset_lab.ai.repository.TpoResultRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
    private final TpoResultClothesRepository tpoResultClothesRepository;

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

        // 모든 요청의 결과를 한 번에 조회 (N+1 방지)
        List<TpoResult> allResults = requests.isEmpty() ? List.of() : tpoResultRepository.findByTpoRequestIn(requests);

        // 결과별 옷 목록을 한 번에 조회 (N+1 방지)
        Map<Long, List<Long>> clothesMap = buildClothesMap(allResults);

        List<SessionHistoryResponse.TurnHistory> turns = requests.stream()
                .map(req -> toTurnHistory(req, allResults, clothesMap))
                .toList();

        return SessionHistoryResponse.builder()
                .sessionId(sessionId)
                .uptoTurnNo(uptoTurnNo)
                .turns(turns)
                .build();
    }

    private Map<Long, List<Long>> buildClothesMap(List<TpoResult> results) {
        if (results.isEmpty()) {
            return Map.of();
        }
        return tpoResultClothesRepository.findByTpoResultIn(results).stream()
                .collect(Collectors.groupingBy(
                        rc -> rc.getTpoResult().getId(),
                        Collectors.mapping(rc -> rc.getClothes().getId(), Collectors.toList())));
    }

    private SessionHistoryResponse.TurnHistory toTurnHistory(
            TpoRequest request, List<TpoResult> allResults, Map<Long, List<Long>> clothesMap) {

        List<SessionHistoryResponse.OutfitDetail> outfits = allResults.stream()
                .filter(r -> r.getTpoRequest().getId().equals(request.getId()))
                .map(r -> SessionHistoryResponse.OutfitDetail.builder()
                        .resultId(r.getId())
                        .clothesIds(clothesMap.getOrDefault(r.getId(), List.of()))
                        .reaction(r.getReaction().name())
                        .vtonImageUrl(r.getVtonImageUrl())
                        .build())
                .toList();

        return SessionHistoryResponse.TurnHistory.builder()
                .turnNo(request.getTurnNo())
                .requestText(request.getRequestText())
                .querySummary(request.getQuerySummary())
                .outfits(outfits)
                .createdAt(request.getCreatedAt())
                .build();
    }
}
