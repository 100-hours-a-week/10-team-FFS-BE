package com.example.kloset_lab.ai.fixture;

import com.example.kloset_lab.ai.dto.ShopRecommendationResponse;
import com.example.kloset_lab.ai.entity.CordiRequest;
import com.example.kloset_lab.ai.entity.CordiResult;
import com.example.kloset_lab.global.ai.http.dto.ShopResponse;
import com.example.kloset_lab.user.entity.Provider;
import com.example.kloset_lab.user.entity.User;
import java.util.List;
import org.springframework.test.util.ReflectionTestUtils;

/** AI 도메인 테스트 데이터 팩토리 */
public class AiFixture {

    public static final Long USER_ID = 1L;
    public static final Long OTHER_USER_ID = 2L;
    public static final Long REQUEST_ID = 20L;
    public static final Long RESULT_ID = 10L;
    public static final String QUERY = "가을 데이트 코디 추천해줘";
    public static final String QUERY_SUMMARY = "가을 감성 캐주얼 코디";
    public static final String OUTFIT_ID = "outfit_s001";
    public static final String TOP_LINK = "https://example.com/top-link.html";
    public static final String BOTTOM_LINK = "https://example.com/bottom-link.html";

    /** 테스트용 User 생성 (id는 ReflectionTestUtils로 설정) */
    public static User testUser(Long userId) {
        User user = User.builder()
                .provider(Provider.KAKAO)
                .providerId("test-" + userId)
                .build();
        ReflectionTestUtils.setField(user, "id", userId);
        return user;
    }

    /** REQUEST_ID가 설정된 CordiRequest 생성 */
    public static CordiRequest cordiRequest(User user) {
        CordiRequest req = CordiRequest.builder().user(user).requestText(QUERY).build();
        ReflectionTestUtils.setField(req, "id", REQUEST_ID);
        return req;
    }

    /** RESULT_ID가 설정된 CordiResult 생성 (reaction = NONE) */
    public static CordiResult cordiResult(CordiRequest req) {
        CordiResult result = CordiResult.builder()
                .cordiRequest(req)
                .querySummary(QUERY_SUMMARY)
                .build();
        ReflectionTestUtils.setField(result, "id", RESULT_ID);
        return result;
    }

    /** 1개 outfit(2개 상품)을 포함하는 ShopResponse 생성 */
    public static ShopResponse shopResponse() {
        return ShopResponse.builder()
                .querySummary(QUERY_SUMMARY)
                .outfits(List.of(ShopResponse.ShopOutfit.builder()
                        .outfitId(OUTFIT_ID)
                        .items(List.of(
                                ShopResponse.ShopItem.builder()
                                        .title("테스트 상의")
                                        .brand("테스트 브랜드")
                                        .price(29000)
                                        .imageUrl("https://example.com/top.jpg")
                                        .link(TOP_LINK)
                                        .category("상의")
                                        .build(),
                                ShopResponse.ShopItem.builder()
                                        .title("테스트 하의")
                                        .brand("테스트 브랜드")
                                        .price(39000)
                                        .imageUrl("https://example.com/bottom.jpg")
                                        .link(BOTTOM_LINK)
                                        .category("하의")
                                        .build()))
                        .build()))
                .build();
    }

    /** 컨트롤러 테스트에서 서비스 stub용 ShopRecommendationResponse 생성 */
    public static ShopRecommendationResponse shopRecommendationResponse() {
        return ShopRecommendationResponse.builder()
                .outfitSummary(QUERY_SUMMARY)
                .outfits(List.of(ShopRecommendationResponse.OutfitItem.builder()
                        .outfitId(OUTFIT_ID)
                        .products(List.of())
                        .feedbackId(RESULT_ID)
                        .build()))
                .build();
    }
}
