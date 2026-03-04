package com.example.kloset_lab.ai.service;

import com.example.kloset_lab.ai.dto.TpoOutfitsResponse;
import com.example.kloset_lab.ai.entity.TpoRequest;
import com.example.kloset_lab.ai.entity.TpoResult;
import com.example.kloset_lab.ai.entity.TpoResultClothes;
import com.example.kloset_lab.ai.infrastructure.http.dto.ClothesDto;
import com.example.kloset_lab.ai.infrastructure.http.dto.OutfitResponse;
import com.example.kloset_lab.ai.repository.TpoRequestRepository;
import com.example.kloset_lab.ai.repository.TpoResultClothesRepository;
import com.example.kloset_lab.ai.repository.TpoResultRepository;
import com.example.kloset_lab.clothes.entity.Clothes;
import com.example.kloset_lab.clothes.repository.ClothesRepository;
import com.example.kloset_lab.global.exception.CustomException;
import com.example.kloset_lab.global.exception.ErrorCode;
import com.example.kloset_lab.media.entity.Purpose;
import com.example.kloset_lab.media.service.MediaService;
import com.example.kloset_lab.media.service.StorageService;
import com.example.kloset_lab.user.entity.User;
import com.example.kloset_lab.user.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * TPO 코디 데이터 저장 전담 서비스.
 *
 * <p>AiService의 generateTpoOutfits()는 AI-BE 호출 동안 트랜잭션을 열지 않으므로, DB 저장과
 * MediaFile 상태 확정은 이 서비스가 별도 트랜잭션으로 처리한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class TpoSaveService {

    private final UserRepository userRepository;
    private final TpoRequestRepository tpoRequestRepository;
    private final TpoResultRepository tpoResultRepository;
    private final TpoResultClothesRepository tpoResultClothesRepository;
    private final ClothesRepository clothesRepository;
    private final StorageService storageService;
    private final MediaService mediaService;

    /**
     * AI 응답 수신 후 TpoRequest/TpoResult 저장, MediaFile 상태 확정, 응답 DTO 빌드
     *
     * @param userId 요청 사용자 ID
     * @param requestText 사용자 입력 텍스트
     * @param outfitResponse AI 서버 응답
     * @return FE 응답 DTO
     */
    public TpoOutfitsResponse saveAndBuild(Long userId, String requestText, OutfitResponse outfitResponse) {
        List<Long> fileIds = outfitResponse.outfits().stream()
                .map(OutfitResponse.Outfit::fileId)
                .toList();
        mediaService.confirmFileUpload(userId, Purpose.OUTFIT, fileIds);

        User user = userRepository.getReferenceById(userId);
        TpoRequest tpoRequest = tpoRequestRepository.save(TpoRequest.builder()
                .user(user)
                .requestText(requestText)
                .querySummary(outfitResponse.querySummary())
                .build());

        List<TpoResult> tpoResults = tpoResultRepository.saveAll(outfitResponse.outfits().stream()
                .map(outfit -> TpoResult.builder()
                        .tpoRequest(tpoRequest)
                        .cordiExplainText(outfit.description())
                        .outfitId(outfit.outfitId())
                        .build())
                .toList());

        List<TpoOutfitsResponse.OutfitItem> outfitItems = new ArrayList<>();
        for (int i = 0; i < outfitResponse.outfits().size(); i++) {
            OutfitResponse.Outfit outfit = outfitResponse.outfits().get(i);
            TpoResult tpoResult = tpoResults.get(i);

            List<Clothes> clothesList = outfit.clothesIds().stream()
                    .map(id -> clothesRepository
                            .findById(id)
                            .orElseThrow(() -> new CustomException(ErrorCode.CLOTHES_NOT_FOUND)))
                    .toList();

            tpoResultClothesRepository.saveAll(clothesList.stream()
                    .map(clothes -> TpoResultClothes.builder()
                            .tpoResult(tpoResult)
                            .clothes(clothes)
                            .build())
                    .toList());

            ClothesDto[] clothesDtos = clothesList.stream()
                    .map(clothes -> ClothesDto.builder()
                            .clothesId(clothes.getId())
                            .imageUrl(storageService.getFullImageUrl(
                                    clothes.getFile().getObjectKey()))
                            .name(clothes.getClothesName())
                            .build())
                    .toArray(ClothesDto[]::new);

            String imageUrl = outfit.fileId() != null ? mediaService.getFileFullUrl(outfit.fileId()) : null;
            outfitItems.add(TpoOutfitsResponse.OutfitItem.builder()
                    .outfitId(tpoResult.getId())
                    .aiComment(outfit.description()
                            + Optional.ofNullable(outfit.fallbackNotice())
                                    .map(notice -> " " + notice)
                                    .orElse(""))
                    .clothes(clothesDtos)
                    .imageUrl(imageUrl)
                    .build());
        }

        return TpoOutfitsResponse.builder()
                .outfitSummary(outfitResponse.querySummary())
                .outfits(outfitItems)
                .build();
    }
}
