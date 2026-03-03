package com.example.kloset_lab.ai.service;

import com.example.kloset_lab.ai.dto.TpoOutfitsResponse;
import com.example.kloset_lab.ai.entity.TpoRequest;
import com.example.kloset_lab.ai.entity.TpoResult;
import com.example.kloset_lab.ai.entity.TpoResultClothes;
import com.example.kloset_lab.ai.repository.TpoRequestRepository;
import com.example.kloset_lab.ai.repository.TpoResultClothesRepository;
import com.example.kloset_lab.ai.repository.TpoResultRepository;
import com.example.kloset_lab.clothes.entity.Clothes;
import com.example.kloset_lab.clothes.repository.ClothesRepository;
import com.example.kloset_lab.global.ai.http.dto.ClothesDto;
import com.example.kloset_lab.global.ai.http.dto.OutfitResponse;
import com.example.kloset_lab.global.exception.CustomException;
import com.example.kloset_lab.global.exception.ErrorCode;
import com.example.kloset_lab.media.service.MediaService;
import com.example.kloset_lab.media.service.StorageService;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * TPO 코디 데이터 저장 전담 서비스.
 *
 * <p>AiService의 generateTpoOutfits()는 AI-BE 호출 동안 트랜잭션을 열지 않으므로, DB 저장은 이 서비스가 별도 트랜잭션으로 처리한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class TpoSaveService {

    private final TpoRequestRepository tpoRequestRepository;
    private final TpoResultRepository tpoResultRepository;
    private final TpoResultClothesRepository tpoResultClothesRepository;
    private final ClothesRepository clothesRepository;
    private final StorageService storageService;
    private final MediaService mediaService;

    /**
     * tpo_request / tpo_result / tpo_result_clothes 저장 후 응답 DTO 빌드
     *
     * @param tpoRequest 저장할 TPO 요청 엔티티 (querySummary 포함)
     * @param outfitResponse AI-BE 응답
     * @return FE 응답 DTO
     */
    public TpoOutfitsResponse saveAndBuild(TpoRequest tpoRequest, OutfitResponse outfitResponse) {
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
                            .imageUrl(mediaService.getFileFullUrl(outfit.fileId()))
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
}
