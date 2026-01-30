package com.example.kloset_lab.ai.service;

import com.example.kloset_lab.ai.dto.TpoFeedbackRequest;
import com.example.kloset_lab.ai.dto.TpoOutfitsRequest;
import com.example.kloset_lab.ai.dto.TpoOutfitsResponse;
import com.example.kloset_lab.ai.dto.TpoRequestHistoryResponse;
import com.example.kloset_lab.ai.entity.TpoRequest;
import com.example.kloset_lab.ai.entity.TpoResult;
import com.example.kloset_lab.ai.entity.TpoResultClothes;
import com.example.kloset_lab.ai.repository.TpoRequestRepository;
import com.example.kloset_lab.ai.repository.TpoResultClothesRepository;
import com.example.kloset_lab.ai.repository.TpoResultRepository;
import com.example.kloset_lab.clothes.entity.Clothes;
import com.example.kloset_lab.clothes.repository.ClothesRepository;
import com.example.kloset_lab.global.ai.client.AIClient;
import com.example.kloset_lab.global.ai.dto.ClothesDto;
import com.example.kloset_lab.global.ai.dto.OutfitResponse;
import com.example.kloset_lab.global.exception.CustomException;
import com.example.kloset_lab.global.exception.ErrorCode;
import com.example.kloset_lab.media.service.StorageService;
import com.example.kloset_lab.user.entity.User;
import com.example.kloset_lab.user.repository.UserRepository;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiService {

    private final AIClient aIClient;
    private final UserRepository userRepository;
    private final TpoRequestRepository tpoRequestRepository;
    private final TpoResultRepository tpoResultRepository;
    private final TpoResultClothesRepository tpoResultClothesRepository;
    private final ClothesRepository clothesRepository;
    private final StorageService storageService;

    /**
     * TPO 코디 생성 요청
     *
     * @param userId 현재 로그인한 사용자 ID
     * @param request TPO 코디 생성 요청 내용 DTO
     * @return 생성된 TPO 코디 결과
     */
    @Transactional
    public TpoOutfitsResponse generateTpoOutfits(Long userId, @Valid TpoOutfitsRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        TpoRequest tpoRequest =
                TpoRequest.builder().user(user).requestText(request.content()).build();

        OutfitResponse outfitResponse = aIClient.recommendOutfit(user.getId(), tpoRequest.getRequestText());
        tpoRequest.addQuerySummary(outfitResponse.querySummary());

        tpoRequestRepository.save(tpoRequest);

        List<TpoResult> tpoResults = saveTpoResults(outfitResponse, tpoRequest);
        saveTpoResultClothes(outfitResponse, tpoResults);

        List<TpoOutfitsResponse.OutfitItem> outfitItems = buildOutfitItems(outfitResponse, tpoResults);

        return TpoOutfitsResponse.builder()
                .outfitSummary(outfitResponse.querySummary())
                .outfits(outfitItems)
                .build();
    }

    /**
     * TPO 코디 결과 엔티티 생성 및 저장
     *
     * @param outfitResponse AI 서버 응답
     * @param tpoRequest TPO 요청 엔티티
     * @return 저장된 TpoResult 리스트
     */
    private List<TpoResult> saveTpoResults(OutfitResponse outfitResponse, TpoRequest tpoRequest) {
        List<TpoResult> tpoResults = outfitResponse.outfits().stream()
                .map(outfit -> TpoResult.builder()
                        .tpoRequest(tpoRequest)
                        .cordiExplainText(outfit.description())
                        .outfitId(outfit.outfitId())
                        .build())
                .toList();

        return tpoResultRepository.saveAll(tpoResults);
    }

    /**
     * TPO 코디에 포함된 의류 정보 생성 및 저장
     *
     * @param outfitResponse AI 서버 응답
     * @param tpoResults 저장된 TpoResult 리스트
     */
    private void saveTpoResultClothes(OutfitResponse outfitResponse, List<TpoResult> tpoResults) {
        for (int i = 0; i < outfitResponse.outfits().size(); i++) {
            OutfitResponse.Outfit outfit = outfitResponse.outfits().get(i);
            TpoResult tpoResult = tpoResults.get(i);

            List<TpoResultClothes> tpoResultClothes = outfit.clothesIds().stream()
                    .map(clothesId -> {
                        Clothes clothes = clothesRepository
                                .findById(clothesId)
                                .orElseThrow(() -> new CustomException(ErrorCode.CLOTHES_NOT_FOUND));

                        return TpoResultClothes.builder()
                                .tpoResult(tpoResult)
                                .clothes(clothes)
                                .build();
                    })
                    .toList();

            tpoResultClothesRepository.saveAll(tpoResultClothes);
        }
    }

    /**
     * 응답용 OutfitItem 리스트 생성
     *
     * @param outfitResponse AI 서버 응답
     * @param tpoResults 저장된 TpoResult 리스트
     * @return OutfitItem 리스트
     */
    private List<TpoOutfitsResponse.OutfitItem> buildOutfitItems(
            OutfitResponse outfitResponse, List<TpoResult> tpoResults) {
        return outfitResponse.outfits().stream()
                .map(outfit -> {
                    int outfitIndex = outfitResponse.outfits().indexOf(outfit);
                    TpoResult tpoResult = tpoResults.get(outfitIndex);

                    ClothesDto[] clothesDtos = outfit.clothesIds().stream()
                            .map(clothesId -> {
                                Clothes clothes = clothesRepository
                                        .findById(clothesId)
                                        .orElseThrow(() -> new CustomException(ErrorCode.CLOTHES_NOT_FOUND));
                                String clothesImageUrl = storageService.getFullImageUrl(
                                        clothes.getFile().getObjectKey());

                                return ClothesDto.builder()
                                        .clothesId(clothes.getId())
                                        .imageUrl(clothesImageUrl)
                                        .name(clothes.getClothesName())
                                        .build();
                            })
                            .toArray(ClothesDto[]::new);

                    String aiComment = buildAiComment(outfit);

                    return TpoOutfitsResponse.OutfitItem.builder()
                            .outfitId(tpoResult.getId())
                            .aiComment(aiComment)
                            .clothes(clothesDtos)
                            .build();
                })
                .toList();
    }

    /**
     * AI 코멘트 생성 (fallbackNotice 포함)
     *
     * @param outfit AI 서버 응답의 개별 코디 정보
     * @return AI 코멘트 문자열
     */
    private String buildAiComment(OutfitResponse.Outfit outfit) {
        return outfit.description()
                + Optional.ofNullable(outfit.fallbackNotice())
                        .map(notice -> " " + notice)
                        .orElse("");
    }

    /**
     * 최근 TPO 요청 기록 조회 (최근 3개)
     *
     * @param userId 현재 로그인한 사용자 ID
     * @return TPO 요청 기록 리스트 (없으면 빈 리스트)
     */
    public TpoRequestHistoryResponse getRecentTpoRequests(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        List<TpoRequest> tpoRequests = tpoRequestRepository.findByUserOrderByCreatedAtDesc(user, PageRequest.of(0, 3));

        List<TpoRequestHistoryResponse.RequestHistory> requestHistories = tpoRequests.stream()
                .map(tpoRequest -> TpoRequestHistoryResponse.RequestHistory.builder()
                        .requestId(tpoRequest.getId())
                        .content(tpoRequest.getRequestText())
                        .build())
                .toList();

        return TpoRequestHistoryResponse.builder()
                .requestHistories(requestHistories)
                .build();
    }

    /**
     * TPO 결과 피드백 등록
     *
     * @param resultId TPO 결과 ID
     * @param request 피드백 요청 DTO
     */
    @Transactional
    public void recordReaction(Long resultId, @Valid TpoFeedbackRequest request) {
        TpoResult tpoResult = tpoResultRepository
                .findById(resultId)
                .orElseThrow(() -> new CustomException(ErrorCode.TPO_RESULT_NOT_FOUND));

        tpoResult.updateReaction(request.reaction());
    }
}
