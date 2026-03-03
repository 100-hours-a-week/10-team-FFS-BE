package com.example.kloset_lab.ai.service;

import com.example.kloset_lab.ai.dto.ShopRecommendationResponse;
import com.example.kloset_lab.ai.entity.CordiRequest;
import com.example.kloset_lab.ai.entity.CordiResult;
import com.example.kloset_lab.ai.entity.Reaction;
import com.example.kloset_lab.ai.infrastructure.http.dto.ShopResponse;
import com.example.kloset_lab.ai.repository.CordiRequestRepository;
import com.example.kloset_lab.ai.repository.CordiResultRepository;
import com.example.kloset_lab.global.exception.CustomException;
import com.example.kloset_lab.global.exception.ErrorCode;
import com.example.kloset_lab.user.entity.User;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 쇼핑 코디 데이터 저장 전담 서비스.
 *
 * <p>AiService의 searchShopOutfits()는 AI-BE 호출 동안 트랜잭션을 열지 않으므로, DB 저장은 이 서비스가 별도 트랜잭션으로 처리한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CordiSaveService {

    private final CordiRequestRepository cordiRequestRepository;
    private final CordiResultRepository cordiResultRepository;

    /**
     * cordi_request / cordi_result 저장 후 응답 DTO 빌드
     *
     * @param user 요청한 사용자 엔티티
     * @param requestText 사용자 입력 텍스트
     * @param shopResponse AI-BE 응답
     * @return FE 응답 DTO
     */
    public ShopRecommendationResponse saveAndBuild(User user, String requestText, ShopResponse shopResponse) {
        CordiRequest cordiRequest = cordiRequestRepository.save(
                CordiRequest.builder().user(user).requestText(requestText).build());

        List<ShopRecommendationResponse.OutfitItem> outfitItems = shopResponse.outfits().stream()
                .map(outfit -> {
                    CordiResult cordiResult = cordiResultRepository.save(CordiResult.builder()
                            .cordiRequest(cordiRequest)
                            .querySummary(shopResponse.querySummary())
                            .build());

                    List<ShopRecommendationResponse.ProductItem> products =
                            Optional.ofNullable(outfit.items()).orElse(List.of()).stream()
                                    .map(item -> ShopRecommendationResponse.ProductItem.builder()
                                            .name(item.title())
                                            .price(item.price())
                                            .brandName(item.brand())
                                            .category(item.category())
                                            .imageUrl(item.imageUrl())
                                            .link(item.link())
                                            .build())
                                    .toList();

                    return ShopRecommendationResponse.OutfitItem.builder()
                            .outfitId(outfit.outfitId())
                            .products(products)
                            .feedbackId(cordiResult.getId())
                            .build();
                })
                .toList();

        return ShopRecommendationResponse.builder()
                .outfitSummary(shopResponse.querySummary())
                .outfits(outfitItems)
                .build();
    }

    /**
     * 쇼핑 코디 결과 반응 업데이트 (소유권 검증 포함)
     *
     * @param userId 현재 로그인한 사용자 ID
     * @param resultId cordi_result PK
     * @param reaction 사용자 반응
     */
    public void updateReaction(Long userId, Long resultId, Reaction reaction) {
        CordiResult cordiResult = cordiResultRepository
                .findByIdWithUser(resultId)
                .orElseThrow(() -> new CustomException(ErrorCode.SHOP_RESULT_NOT_FOUND));

        if (!cordiResult.getCordiRequest().getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.SHOP_RESULT_ACCESS_DENIED);
        }

        cordiResult.updateReaction(reaction);
    }
}
