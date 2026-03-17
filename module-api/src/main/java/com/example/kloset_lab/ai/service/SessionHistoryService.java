package com.example.kloset_lab.ai.service;

import com.example.kloset_lab.ai.dto.SessionHistoryResponse;
import com.example.kloset_lab.ai.dto.SessionListResponse;
import com.example.kloset_lab.ai.entity.TpoRequest;
import com.example.kloset_lab.ai.entity.TpoResult;
import com.example.kloset_lab.ai.entity.TpoSession;
import com.example.kloset_lab.ai.repository.TpoRequestRepository;
import com.example.kloset_lab.ai.repository.TpoResultClothesRepository;
import com.example.kloset_lab.ai.repository.TpoResultRepository;
import com.example.kloset_lab.ai.repository.TpoSessionRepository;
import com.example.kloset_lab.global.exception.CustomException;
import com.example.kloset_lab.global.exception.ErrorCode;
import com.example.kloset_lab.user.entity.User;
import com.example.kloset_lab.user.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 세션 조회 서비스 (세션 목록 + 세션 상세 + Internal API)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SessionHistoryService {

    private final UserRepository userRepository;
    private final TpoSessionRepository tpoSessionRepository;
    private final TpoRequestRepository tpoRequestRepository;
    private final TpoResultRepository tpoResultRepository;
    private final TpoResultClothesRepository tpoResultClothesRepository;

    /**
     * 사용자의 세션 목록을 최근 활동순으로 조회한다. (페이징)
     *
     * @param userId 사용자 ID
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 세션 목록 응답
     */
    public SessionListResponse getSessionList(Long userId, int page, int size) {
        User user = userRepository.findById(userId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Slice<TpoSession> sessionSlice =
                tpoSessionRepository.findByUserOrderByLastActivityAtDesc(user, PageRequest.of(page, size));

        List<TpoSession> sessions = sessionSlice.getContent();
        if (sessions.isEmpty()) {
            return SessionListResponse.builder()
                    .sessions(List.of())
                    .hasNext(false)
                    .build();
        }

        // 세션별 첫 번째 턴 요청을 배치 조회 (N+1 방지)
        Map<Long, String> titleMap = tpoRequestRepository.findByTpoSessionInAndTurnNo(sessions, 1).stream()
                .collect(Collectors.toMap(r -> r.getTpoSession().getId(), TpoRequest::getRequestText, (a, b) -> a));

        List<SessionListResponse.SessionPreview> previews = sessions.stream()
                .map(s -> SessionListResponse.SessionPreview.builder()
                        .sessionId(s.getSessionId())
                        .title(titleMap.getOrDefault(s.getId(), ""))
                        .lastActivityAt(s.getLastActivityAt())
                        .build())
                .toList();

        return SessionListResponse.builder()
                .sessions(previews)
                .hasNext(sessionSlice.hasNext())
                .build();
    }

    /**
     * 세션 상세를 최신 턴부터 페이징 조회한다. (FE 채팅 뷰용)
     *
     * <p>PENDING 상태의 턴은 outfits가 빈 배열이며, FE는 status를 보고 로딩 상태를 표시한다.
     *
     * @param sessionId 세션 ID
     * @param userId 사용자 ID
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 세션 히스토리 응답 (최신 턴 우선)
     */
    public SessionHistoryResponse getSessionDetail(String sessionId, Long userId, int page, int size) {
        Slice<TpoRequest> requestSlice =
                tpoRequestRepository.findSessionDetail(sessionId, userId, PageRequest.of(page, size));

        List<TpoRequest> requests = requestSlice.getContent();
        if (requests.isEmpty() && page == 0) {
            throw new CustomException(ErrorCode.SESSION_NOT_FOUND);
        }

        // PENDING 요청은 결과가 없지만 정상 (inflight 상태) — 빈 outfits로 반환
        List<TpoResult> allResults = requests.isEmpty() ? List.of() : tpoResultRepository.findByTpoRequestIn(requests);
        Map<Long, List<Long>> clothesMap = buildClothesMap(allResults);

        List<SessionHistoryResponse.TurnHistory> turns = requests.stream()
                .map(req -> toTurnHistory(req, allResults, clothesMap))
                .toList();

        return SessionHistoryResponse.builder()
                .sessionId(sessionId)
                .uptoTurnNo(requests.isEmpty() ? 0 : requests.getFirst().getTurnNo())
                .turns(turns)
                .hasNext(requestSlice.hasNext())
                .build();
    }

    /**
     * 세션 히스토리를 최신 턴부터 내림차순으로 조회한다. (Internal API용)
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

        List<TpoResult> allResults = requests.isEmpty() ? List.of() : tpoResultRepository.findByTpoRequestIn(requests);
        Map<Long, List<Long>> clothesMap = buildClothesMap(allResults);

        List<SessionHistoryResponse.TurnHistory> turns = requests.stream()
                .map(req -> toTurnHistory(req, allResults, clothesMap))
                .toList();

        return SessionHistoryResponse.builder()
                .sessionId(sessionId)
                .uptoTurnNo(uptoTurnNo)
                .turns(turns)
                .hasNext(false)
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
                .status(request.getStatus() != null ? request.getStatus().name() : null)
                .outfits(outfits)
                .createdAt(request.getCreatedAt())
                .build();
    }
}
