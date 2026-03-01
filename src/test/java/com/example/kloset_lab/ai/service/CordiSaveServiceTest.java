package com.example.kloset_lab.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.example.kloset_lab.ai.dto.ShopRecommendationResponse;
import com.example.kloset_lab.ai.entity.CordiRequest;
import com.example.kloset_lab.ai.entity.CordiResult;
import com.example.kloset_lab.ai.entity.Reaction;
import com.example.kloset_lab.ai.fixture.AiFixture;
import com.example.kloset_lab.ai.repository.CordiRequestRepository;
import com.example.kloset_lab.ai.repository.CordiResultRepository;
import com.example.kloset_lab.global.ai.http.dto.ShopResponse;
import com.example.kloset_lab.global.annotation.ServiceTest;
import com.example.kloset_lab.global.exception.CustomException;
import com.example.kloset_lab.global.exception.ErrorCode;
import com.example.kloset_lab.user.entity.User;
import java.util.List;
import java.util.Optional;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@ServiceTest
@DisplayName("CordiSaveService 단위 테스트")
class CordiSaveServiceTest {

    @Mock
    private CordiRequestRepository cordiRequestRepository;

    @Mock
    private CordiResultRepository cordiResultRepository;

    @InjectMocks
    private CordiSaveService cordiSaveService;

    // ======================== saveAndBuild ========================

    @Nested
    @DisplayName("saveAndBuild")
    class SaveAndBuild {

        @Test
        @DisplayName("정상 — CordiRequest·CordiResult 저장 후 응답이 올바르게 빌드된다")
        void 정상_저장_및_응답_빌드() {
            User user = AiFixture.testUser(AiFixture.USER_ID);
            ShopResponse shopResponse = AiFixture.shopResponse();
            CordiRequest savedRequest = AiFixture.cordiRequest(user);
            CordiResult savedResult = AiFixture.cordiResult(savedRequest);

            given(cordiRequestRepository.save(any(CordiRequest.class))).willReturn(savedRequest);
            given(cordiResultRepository.save(any(CordiResult.class))).willReturn(savedResult);

            ShopRecommendationResponse response = cordiSaveService.saveAndBuild(user, AiFixture.QUERY, shopResponse);

            assertThat(response.outfitSummary()).isEqualTo(AiFixture.QUERY_SUMMARY);
            assertThat(response.outfits()).hasSize(1);
            assertThat(response.outfits().get(0).feedbackId()).isEqualTo(AiFixture.RESULT_ID);
            assertThat(response.outfits().get(0).products()).hasSize(2);
            assertThat(response.outfits().get(0).products().get(0).link()).isEqualTo(AiFixture.TOP_LINK);
            assertThat(response.outfits().get(0).products().get(1).link()).isEqualTo(AiFixture.BOTTOM_LINK);
            then(cordiRequestRepository).should().save(any(CordiRequest.class));
            then(cordiResultRepository).should().save(any(CordiResult.class));
        }

        @Test
        @DisplayName("outfit.items()가 null이면 products를 빈 리스트로 반환한다 (NPE 방어)")
        void items_null_이면_products_빈_리스트() {
            User user = AiFixture.testUser(AiFixture.USER_ID);
            ShopResponse shopResponse = ShopResponse.builder()
                    .querySummary(AiFixture.QUERY_SUMMARY)
                    .outfits(List.of(ShopResponse.ShopOutfit.builder()
                            .outfitId(AiFixture.OUTFIT_ID)
                            .items(null)
                            .build()))
                    .build();
            CordiRequest savedRequest = AiFixture.cordiRequest(user);
            CordiResult savedResult = AiFixture.cordiResult(savedRequest);

            given(cordiRequestRepository.save(any(CordiRequest.class))).willReturn(savedRequest);
            given(cordiResultRepository.save(any(CordiResult.class))).willReturn(savedResult);

            ShopRecommendationResponse response = cordiSaveService.saveAndBuild(user, AiFixture.QUERY, shopResponse);

            assertThat(response.outfits().get(0).products()).isEmpty();
        }

        @Test
        @DisplayName("outfit.items()가 빈 리스트면 products를 빈 리스트로 반환한다")
        void items_empty_이면_products_빈_리스트() {
            User user = AiFixture.testUser(AiFixture.USER_ID);
            ShopResponse shopResponse = ShopResponse.builder()
                    .querySummary(AiFixture.QUERY_SUMMARY)
                    .outfits(List.of(ShopResponse.ShopOutfit.builder()
                            .outfitId(AiFixture.OUTFIT_ID)
                            .items(List.of())
                            .build()))
                    .build();
            CordiRequest savedRequest = AiFixture.cordiRequest(user);
            CordiResult savedResult = AiFixture.cordiResult(savedRequest);

            given(cordiRequestRepository.save(any(CordiRequest.class))).willReturn(savedRequest);
            given(cordiResultRepository.save(any(CordiResult.class))).willReturn(savedResult);

            ShopRecommendationResponse response = cordiSaveService.saveAndBuild(user, AiFixture.QUERY, shopResponse);

            assertThat(response.outfits().get(0).products()).isEmpty();
        }
    }

    // ======================== updateReaction ========================

    @Nested
    @DisplayName("updateReaction")
    class UpdateReaction {

        @Test
        @DisplayName("정상 — GOOD 반응으로 업데이트되면 CordiResult.reaction이 GOOD으로 변경된다")
        void 정상_GOOD_업데이트() {
            User user = AiFixture.testUser(AiFixture.USER_ID);
            CordiRequest savedRequest = AiFixture.cordiRequest(user);
            CordiResult savedResult = AiFixture.cordiResult(savedRequest);

            given(cordiResultRepository.findByIdWithUser(AiFixture.RESULT_ID)).willReturn(Optional.of(savedResult));

            cordiSaveService.updateReaction(AiFixture.USER_ID, AiFixture.RESULT_ID, Reaction.GOOD);

            assertThat(savedResult.getReaction()).isEqualTo(Reaction.GOOD);
        }

        @Test
        @DisplayName("resultId에 해당하는 결과가 없으면 SHOP_RESULT_NOT_FOUND 예외")
        void resultId_없으면_SHOP_RESULT_NOT_FOUND() {
            given(cordiResultRepository.findByIdWithUser(AiFixture.RESULT_ID)).willReturn(Optional.empty());

            assertCustomException(
                    () -> cordiSaveService.updateReaction(AiFixture.USER_ID, AiFixture.RESULT_ID, Reaction.GOOD),
                    ErrorCode.SHOP_RESULT_NOT_FOUND);
        }

        @Test
        @DisplayName("다른 사용자의 결과에 접근하면 SHOP_RESULT_ACCESS_DENIED 예외")
        void 다른_사용자_SHOP_RESULT_ACCESS_DENIED() {
            User owner = AiFixture.testUser(AiFixture.USER_ID);
            CordiRequest savedRequest = AiFixture.cordiRequest(owner);
            CordiResult savedResult = AiFixture.cordiResult(savedRequest);

            given(cordiResultRepository.findByIdWithUser(AiFixture.RESULT_ID)).willReturn(Optional.of(savedResult));

            // OTHER_USER_ID(2)로 USER_ID(1) 소유 결과에 접근
            assertCustomException(
                    () -> cordiSaveService.updateReaction(AiFixture.OTHER_USER_ID, AiFixture.RESULT_ID, Reaction.GOOD),
                    ErrorCode.SHOP_RESULT_ACCESS_DENIED);
        }
    }

    // ======================== 헬퍼 ========================

    private void assertCustomException(ThrowingCallable callable, ErrorCode expectedCode) {
        assertThatThrownBy(callable)
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(expectedCode);
    }
}
