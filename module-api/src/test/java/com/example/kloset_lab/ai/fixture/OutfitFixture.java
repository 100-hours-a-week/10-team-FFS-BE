package com.example.kloset_lab.ai.fixture;

import com.example.kloset_lab.ai.entity.Reaction;
import com.example.kloset_lab.ai.entity.TpoRequest;
import com.example.kloset_lab.ai.entity.TpoResult;
import com.example.kloset_lab.ai.entity.TpoSession;
import com.example.kloset_lab.ai.infrastructure.kafka.dto.OutfitKafkaResponse;
import com.example.kloset_lab.user.entity.Provider;
import com.example.kloset_lab.user.entity.User;
import java.util.List;
import org.springframework.test.util.ReflectionTestUtils;

/** 코디추천 비동기 파이프라인 테스트 데이터 팩토리 */
public class OutfitFixture {

    public static final Long USER_ID = 1L;
    public static final Long OTHER_USER_ID = 2L;
    public static final String REQUEST_ID = "req_test_123";
    public static final String SESSION_ID = "sess_test_456";
    public static final String CONTENT = "내일 면접인데 뭐 입지?";
    public static final int TURN_NO = 1;

    public static User testUser(Long userId) {
        User user = User.builder()
                .provider(Provider.KAKAO)
                .providerId("test-" + userId)
                .build();
        ReflectionTestUtils.setField(user, "id", userId);
        return user;
    }

    /** inflight가 없는 세션 생성 (lastTurnNo = 0) */
    public static TpoSession testSession(User user) {
        TpoSession session = TpoSession.builder().user(user).build();
        ReflectionTestUtils.setField(session, "id", 1L);
        ReflectionTestUtils.setField(session, "sessionId", SESSION_ID);
        return session;
    }

    /** inflight 상태인 세션 생성 */
    public static TpoSession inflightSession(User user) {
        TpoSession session = testSession(user);
        session.startInflight(REQUEST_ID);
        return session;
    }

    /** PENDING 상태의 비동기 TpoRequest 생성 */
    public static TpoRequest testRequest(User user, TpoSession session) {
        TpoRequest request = new TpoRequest(user, session, REQUEST_ID, TURN_NO, CONTENT);
        ReflectionTestUtils.setField(request, "id", 10L);
        return request;
    }

    /** COMPLETED 상태의 TpoRequest 생성 */
    public static TpoRequest completedRequest(User user, TpoSession session) {
        TpoRequest request = testRequest(user, session);
        request.complete();
        return request;
    }

    /** 리액션 NONE인 TpoResult 생성 */
    public static TpoResult testResult(TpoRequest request) {
        TpoResult result = TpoResult.builder()
                .tpoRequest(request)
                .cordiExplainText("코디 설명")
                .outfitId("outfit_001")
                .build();
        ReflectionTestUtils.setField(result, "id", 100L);
        return result;
    }

    /** 리액션이 설정된 TpoResult 생성 */
    public static TpoResult reactedResult(TpoRequest request) {
        TpoResult result = testResult(request);
        result.updateReaction(Reaction.GOOD);
        return result;
    }

    /** Kafka 성공 응답 메시지 */
    public static OutfitKafkaResponse successResponse() {
        return new OutfitKafkaResponse(
                REQUEST_ID,
                "success",
                "면접에 적합한 코디를 추천합니다",
                null,
                null,
                List.of(new OutfitKafkaResponse.Outfit(
                        "outfit_uuid_1", "네이비 블레이저와 화이트 셔츠", List.of(1L, 2L), null, null)),
                new OutfitKafkaResponse.Metadata(false, false, 12400),
                null,
                null,
                "2025-02-27T10:00:12Z");
    }

    /** Kafka 실패 응답 메시지 */
    public static OutfitKafkaResponse failedResponse() {
        return new OutfitKafkaResponse(
                REQUEST_ID,
                "failed",
                null,
                null,
                null,
                null,
                null,
                new OutfitKafkaResponse.Error("INFRA_FAILURE", "서비스 일시 장애", 60),
                null,
                "2025-02-27T10:00:30Z");
    }

    /** Kafka progress 응답 메시지 */
    public static OutfitKafkaResponse progressResponse() {
        return new OutfitKafkaResponse(
                REQUEST_ID,
                "processing",
                null,
                "query_parsing",
                "의도 분석 중...",
                null,
                null,
                null,
                null,
                "2025-02-27T10:00:02Z");
    }

    /** Kafka clarification_needed 응답 메시지 */
    public static OutfitKafkaResponse clarificationNeededResponse() {
        return new OutfitKafkaResponse(
                REQUEST_ID,
                "clarification_needed",
                null,
                null,
                null,
                null,
                null,
                null,
                "죄송해요, 요청을 정확히 이해하지 못했어요. 어떤 코디를 어떻게 바꾸고 싶으신가요?",
                "2025-02-27T10:00:01Z");
    }
}
