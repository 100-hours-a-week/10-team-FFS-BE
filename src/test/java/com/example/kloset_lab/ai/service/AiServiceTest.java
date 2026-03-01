package com.example.kloset_lab.ai.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.verifyNoInteractions;

import com.example.kloset_lab.ai.dto.ShopRecommendationRequest;
import com.example.kloset_lab.ai.dto.TpoFeedbackRequest;
import com.example.kloset_lab.ai.entity.Reaction;
import com.example.kloset_lab.ai.fixture.AiFixture;
import com.example.kloset_lab.ai.repository.TpoRequestRepository;
import com.example.kloset_lab.ai.repository.TpoResultClothesRepository;
import com.example.kloset_lab.ai.repository.TpoResultRepository;
import com.example.kloset_lab.clothes.repository.ClothesRepository;
import com.example.kloset_lab.global.ai.http.client.AIClient;
import com.example.kloset_lab.global.ai.http.dto.ShopResponse;
import com.example.kloset_lab.global.annotation.ServiceTest;
import com.example.kloset_lab.global.exception.CustomException;
import com.example.kloset_lab.global.exception.ErrorCode;
import com.example.kloset_lab.media.service.MediaService;
import com.example.kloset_lab.media.service.StorageService;
import com.example.kloset_lab.user.entity.User;
import com.example.kloset_lab.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@ServiceTest
@DisplayName("AiService 단위 테스트 (쇼핑 코디 검색)")
class AiServiceTest {

    @Mock
    private AIClient aIClient;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TpoRequestRepository tpoRequestRepository;

    @Mock
    private TpoResultRepository tpoResultRepository;

    @Mock
    private TpoResultClothesRepository tpoResultClothesRepository;

    @Mock
    private ClothesRepository clothesRepository;

    @Mock
    private StorageService storageService;

    @Mock
    private MediaService mediaService;

    @Mock
    private CordiSaveService cordiSaveService;

    @InjectMocks
    private AiService aiService;

    // ======================== searchShopOutfits ========================

    @Nested
    @DisplayName("searchShopOutfits")
    class SearchShopOutfits {

        @Test
        @DisplayName("userId에 해당하는 사용자가 없으면 USER_NOT_FOUND 예외 (AI 호출 없음)")
        void userId_없으면_USER_NOT_FOUND() {
            given(userRepository.findById(AiFixture.USER_ID)).willReturn(Optional.empty());

            assertCustomException(
                    () -> aiService.searchShopOutfits(
                            AiFixture.USER_ID, new ShopRecommendationRequest(AiFixture.QUERY)),
                    ErrorCode.USER_NOT_FOUND);

            verifyNoInteractions(aIClient);
        }

        @Test
        @DisplayName("AI 응답이 null이면 INSUFFICIENT_ITEMS 예외 (saveAndBuild 호출 없음)")
        void AI_응답_null_이면_INSUFFICIENT_ITEMS() {
            User user = AiFixture.testUser(AiFixture.USER_ID);
            given(userRepository.findById(AiFixture.USER_ID)).willReturn(Optional.of(user));
            given(aIClient.searchShop(anyLong(), anyString())).willReturn(null);

            assertCustomException(
                    () -> aiService.searchShopOutfits(
                            AiFixture.USER_ID, new ShopRecommendationRequest(AiFixture.QUERY)),
                    ErrorCode.INSUFFICIENT_ITEMS);

            verifyNoInteractions(cordiSaveService);
        }

        @Test
        @DisplayName("AI 응답 outfits가 빈 리스트면 INSUFFICIENT_ITEMS 예외 (saveAndBuild 호출 없음)")
        void AI_응답_outfits_empty_이면_INSUFFICIENT_ITEMS() {
            User user = AiFixture.testUser(AiFixture.USER_ID);
            ShopResponse emptyResponse = ShopResponse.builder()
                    .querySummary(AiFixture.QUERY_SUMMARY)
                    .outfits(List.of())
                    .build();

            given(userRepository.findById(AiFixture.USER_ID)).willReturn(Optional.of(user));
            given(aIClient.searchShop(anyLong(), anyString())).willReturn(emptyResponse);

            assertCustomException(
                    () -> aiService.searchShopOutfits(
                            AiFixture.USER_ID, new ShopRecommendationRequest(AiFixture.QUERY)),
                    ErrorCode.INSUFFICIENT_ITEMS);

            verifyNoInteractions(cordiSaveService);
        }

        @Test
        @DisplayName("정상 — cordiSaveService.saveAndBuild()에 위임하고 결과를 반환한다")
        void 정상_saveAndBuild_위임() {
            User user = AiFixture.testUser(AiFixture.USER_ID);
            ShopResponse shopResponse = AiFixture.shopResponse();

            given(userRepository.findById(AiFixture.USER_ID)).willReturn(Optional.of(user));
            given(aIClient.searchShop(anyLong(), anyString())).willReturn(shopResponse);
            given(cordiSaveService.saveAndBuild(any(User.class), anyString(), any(ShopResponse.class)))
                    .willReturn(AiFixture.shopRecommendationResponse());

            aiService.searchShopOutfits(AiFixture.USER_ID, new ShopRecommendationRequest(AiFixture.QUERY));

            then(cordiSaveService).should().saveAndBuild(user, AiFixture.QUERY, shopResponse);
        }
    }

    // ======================== recordShopReaction ========================

    @Nested
    @DisplayName("recordShopReaction")
    class RecordShopReaction {

        @Test
        @DisplayName("정상 — cordiSaveService.updateReaction()에 위임한다")
        void 정상_updateReaction_위임() {
            TpoFeedbackRequest request = new TpoFeedbackRequest(Reaction.GOOD);

            aiService.recordShopReaction(AiFixture.USER_ID, AiFixture.RESULT_ID, request);

            then(cordiSaveService).should().updateReaction(AiFixture.USER_ID, AiFixture.RESULT_ID, Reaction.GOOD);
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
