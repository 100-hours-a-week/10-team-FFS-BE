package com.example.kloset_lab.ai.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.kloset_lab.ai.dto.ShopRecommendationRequest;
import com.example.kloset_lab.ai.dto.TpoFeedbackRequest;
import com.example.kloset_lab.ai.fixture.AiFixture;
import com.example.kloset_lab.ai.service.AiService;
import com.example.kloset_lab.global.base.ControllerTest;
import com.example.kloset_lab.global.exception.CustomException;
import com.example.kloset_lab.global.exception.ErrorCode;
import com.example.kloset_lab.global.response.Message;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(AiController.class)
@DisplayName("AiController 슬라이스 테스트 (쇼핑 코디)")
class AiControllerTest extends ControllerTest {

    @MockitoBean
    private AiService aiService;

    // ──────────────────────────────────────────────────────────────────────────
    // POST /api/v2/product-recommendations — 쇼핑 코디 추천 요청
    // ──────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("POST /api/v2/product-recommendations")
    class SearchShopOutfits {

        @Test
        @DisplayName("인증 없이 요청하면 401을 반환한다")
        void 인증_없으면_401() throws Exception {
            mockMvc.perform(post("/api/v2/product-recommendations")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"content\":\"가을 데이트 코디\"}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("content 필드가 없으면 400을 반환한다")
        void content_없으면_400() throws Exception {
            mockMvc.perform(withAuth(post("/api/v2/product-recommendations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400));
        }

        @Test
        @DisplayName("content가 1자이면 400을 반환한다 (@Size min=2)")
        void content_1자_이면_400() throws Exception {
            mockMvc.perform(withAuth(post("/api/v2/product-recommendations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"content\":\"가\"}")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400));
        }

        @Test
        @DisplayName("content가 101자이면 400을 반환한다 (@Size max=100)")
        void content_101자_이면_400() throws Exception {
            String oversized = "가".repeat(101);
            mockMvc.perform(withAuth(post("/api/v2/product-recommendations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"content\":\"" + oversized + "\"}")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400));
        }

        @Test
        @DisplayName("정상 요청이면 200과 products_fetched 메시지를 반환한다")
        void 정상_요청_200() throws Exception {
            given(aiService.searchShopOutfits(anyLong(), any(ShopRecommendationRequest.class)))
                    .willReturn(AiFixture.shopRecommendationResponse());

            mockMvc.perform(withAuth(post("/api/v2/product-recommendations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"content\":\"가을 데이트 코디\"}")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value(Message.PRODUCTS_FETCHED))
                    .andExpect(jsonPath("$.data.outfitSummary").value(AiFixture.QUERY_SUMMARY))
                    .andExpect(jsonPath("$.data.outfits").isArray())
                    .andExpect(jsonPath("$.data.outfits[0].feedbackId").value(AiFixture.RESULT_ID));
        }

        @Test
        @DisplayName("서비스에서 INSUFFICIENT_ITEMS가 발생하면 422를 반환한다")
        void 서비스_예외_INSUFFICIENT_ITEMS_422() throws Exception {
            given(aiService.searchShopOutfits(anyLong(), any(ShopRecommendationRequest.class)))
                    .willThrow(new CustomException(ErrorCode.INSUFFICIENT_ITEMS));

            mockMvc.perform(withAuth(post("/api/v2/product-recommendations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"content\":\"가을 데이트 코디\"}")))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.code").value(422))
                    .andExpect(jsonPath("$.message").value(ErrorCode.INSUFFICIENT_ITEMS.getMessage()));
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // PATCH /api/v2/product-recommendations/feedbacks/{resultId} — 쇼핑 피드백 등록
    // ──────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("PATCH /api/v2/product-recommendations/feedbacks/{resultId}")
    class RecordShopReaction {

        @Test
        @DisplayName("인증 없이 요청하면 401을 반환한다")
        void 인증_없으면_401() throws Exception {
            mockMvc.perform(patch("/api/v2/product-recommendations/feedbacks/1")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reaction\":\"GOOD\"}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("reaction 필드가 없으면 400을 반환한다")
        void reaction_없으면_400() throws Exception {
            mockMvc.perform(withAuth(patch("/api/v2/product-recommendations/feedbacks/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400));
        }

        @Test
        @DisplayName("정상 요청이면 200과 reaction_recorded 메시지를 반환한다")
        void 정상_요청_200() throws Exception {
            willDoNothing().given(aiService).recordShopReaction(anyLong(), anyLong(), any(TpoFeedbackRequest.class));

            mockMvc.perform(withAuth(patch("/api/v2/product-recommendations/feedbacks/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reaction\":\"GOOD\"}")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value(Message.REACTION_RECORDED));
        }

        @Test
        @DisplayName("서비스에서 SHOP_RESULT_NOT_FOUND가 발생하면 404를 반환한다")
        void 서비스_예외_NOT_FOUND_404() throws Exception {
            willThrow(new CustomException(ErrorCode.SHOP_RESULT_NOT_FOUND))
                    .given(aiService)
                    .recordShopReaction(anyLong(), anyLong(), any(TpoFeedbackRequest.class));

            mockMvc.perform(withAuth(patch("/api/v2/product-recommendations/feedbacks/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reaction\":\"GOOD\"}")))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(404))
                    .andExpect(jsonPath("$.message").value(ErrorCode.SHOP_RESULT_NOT_FOUND.getMessage()));
        }

        @Test
        @DisplayName("서비스에서 SHOP_RESULT_ACCESS_DENIED가 발생하면 403을 반환한다")
        void 서비스_예외_ACCESS_DENIED_403() throws Exception {
            willThrow(new CustomException(ErrorCode.SHOP_RESULT_ACCESS_DENIED))
                    .given(aiService)
                    .recordShopReaction(anyLong(), anyLong(), any(TpoFeedbackRequest.class));

            mockMvc.perform(withAuth(patch("/api/v2/product-recommendations/feedbacks/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reaction\":\"GOOD\"}")))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(403))
                    .andExpect(jsonPath("$.message").value(ErrorCode.SHOP_RESULT_ACCESS_DENIED.getMessage()));
        }
    }
}
