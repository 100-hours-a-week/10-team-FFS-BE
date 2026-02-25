package com.example.kloset_lab.chat.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.kloset_lab.chat.document.ChatMessage;
import com.example.kloset_lab.global.base.MongoRepositoryTest;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@DisplayName("ChatMessageRepository 슬라이스 테스트")
class ChatMessageRepositoryTest extends MongoRepositoryTest {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    private static final Long ROOM_A = 10L;
    private static final Long ROOM_B = 20L;
    private static final Long SENDER = 1L;

    // 시간 순서: T1 < T2 < T3 < T4 < T5 (1시간 간격, ObjectId 비교 보장)
    private static final Instant T1 = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant T2 = Instant.parse("2026-01-01T01:00:00Z");
    private static final Instant T3 = Instant.parse("2026-01-01T02:00:00Z");
    private static final Instant T4 = Instant.parse("2026-01-01T03:00:00Z");
    private static final Instant T5 = Instant.parse("2026-01-01T04:00:00Z");

    // ObjectId 순서: ID1 < ID2 < ID3 < ID4 < ID5
    private static final ObjectId ID1 = new ObjectId(Date.from(T1));
    private static final ObjectId ID2 = new ObjectId(Date.from(T2));
    private static final ObjectId ID3 = new ObjectId(Date.from(T3));
    private static final ObjectId ID4 = new ObjectId(Date.from(T4));
    private static final ObjectId ID5 = new ObjectId(Date.from(T5));

    // ─── 헬퍼 ───────────────────────────────────────────────────────────────

    /** 지정 ID·방·시각으로 텍스트 메시지를 생성한다 */
    private ChatMessage message(Long roomId, ObjectId id, Instant createdAt) {
        return ChatMessage.builder()
                .id(id)
                .roomId(roomId)
                .senderId(SENDER)
                .type("TEXT")
                .content("메시지")
                .createdAt(createdAt)
                .build();
    }

    /** _id 내림차순 PageRequest */
    private PageRequest descById(int size) {
        return PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "_id"));
    }

    private void save(ChatMessage... messages) {
        chatMessageRepository.saveAll(List.of(messages));
    }

    // ─── 테스트 ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("findByRoomId — 첫 페이지 조회 (커서 없음)")
    class FindByRoomId {

        @Test
        @DisplayName("해당 방 메시지만 최신순으로 반환한다")
        void 해당_방_메시지만_최신순_반환() {
            save(message(ROOM_A, ID1, T1), message(ROOM_A, ID2, T2), message(ROOM_B, ID3, T3)); // 다른 방 — 결과에 포함 안 됨

            List<ChatMessage> result = chatMessageRepository.findByRoomId(ROOM_A, descById(10));

            assertThat(result).hasSize(2).extracting(ChatMessage::getId).containsExactly(ID2, ID1); // 최신순
        }

        @Test
        @DisplayName("size+1 조회 시 초과 건이 포함되어 반환된다 (hasNextPage 판별 목적)")
        void size_plus1_조회시_초과건_포함() {
            save(message(ROOM_A, ID1, T1), message(ROOM_A, ID2, T2), message(ROOM_A, ID3, T3));

            // 서비스는 limit+1 개를 요청하여 반환된 건수로 hasNextPage를 판별한다
            List<ChatMessage> result = chatMessageRepository.findByRoomId(ROOM_A, descById(3));

            assertThat(result).hasSize(3);
        }
    }

    @Nested
    @DisplayName("findByRoomIdAndIdLessThan — 커서 기반 페이지네이션")
    class FindByRoomIdAndIdLessThan {

        @Test
        @DisplayName("커서(ObjectId) 미만의 메시지만 최신순으로 반환한다")
        void 커서_미만_메시지_최신순() {
            save(
                    message(ROOM_A, ID1, T1),
                    message(ROOM_A, ID2, T2),
                    message(ROOM_A, ID3, T3), // cursor
                    message(ROOM_A, ID4, T4)); // cursor 초과

            List<ChatMessage> result = chatMessageRepository.findByRoomIdAndIdLessThan(ROOM_A, ID3, descById(10));

            assertThat(result).extracting(ChatMessage::getId).containsExactly(ID2, ID1);
        }

        @Test
        @DisplayName("커서와 동일한 ID는 결과에 포함되지 않는다 (LessThan 경계)")
        void 커서_동일_ID_미포함() {
            save(message(ROOM_A, ID1, T1));

            List<ChatMessage> result = chatMessageRepository.findByRoomIdAndIdLessThan(ROOM_A, ID1, descById(10));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("다른 방 메시지는 결과에 포함되지 않는다")
        void 다른_방_메시지_미포함() {
            save(message(ROOM_A, ID1, T1), message(ROOM_B, ID2, T2)); // 다른 방

            List<ChatMessage> result = chatMessageRepository.findByRoomIdAndIdLessThan(ROOM_A, ID3, descById(10));

            assertThat(result).hasSize(1).extracting(ChatMessage::getId).containsExactly(ID1);
        }
    }

    @Nested
    @DisplayName("findByRoomIdAndCreatedAtGreaterThanEqual — 재진입 첫 페이지")
    class FindByRoomIdAndCreatedAtGreaterThanEqual {

        @Test
        @DisplayName("enteredAt 이상의 메시지만 최신순으로 반환한다")
        void enteredAt_이상_메시지_반환() {
            save(
                    message(ROOM_A, ID1, T1), // T1 — enteredAt(T2) 미만, 제외
                    message(ROOM_A, ID2, T2), // T2 — 경계값, 포함
                    message(ROOM_A, ID3, T3)); // T3 — 포함

            List<ChatMessage> result =
                    chatMessageRepository.findByRoomIdAndCreatedAtGreaterThanEqual(ROOM_A, T2, descById(10));

            assertThat(result).extracting(ChatMessage::getId).containsExactly(ID3, ID2);
        }

        @Test
        @DisplayName("createdAt == enteredAt 경계값은 결과에 포함된다 (GreaterThanEqual)")
        void createdAt_enteredAt_동일_경계_포함() {
            save(message(ROOM_A, ID1, T1));

            List<ChatMessage> result =
                    chatMessageRepository.findByRoomIdAndCreatedAtGreaterThanEqual(ROOM_A, T1, descById(10));

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("findByRoomIdAndIdLessThanAndCreatedAtGreaterThanEqual — 재진입 커서 기반")
    class FindByRoomIdAndIdLessThanAndCreatedAtGreaterThanEqual {

        @Test
        @DisplayName("커서 미만 AND enteredAt 이상 두 조건을 동시에 만족하는 메시지만 반환한다")
        void 두_조건_모두_만족하는_메시지만_반환() {
            // cursor=ID4, enteredAt=T2
            save(
                    message(ROOM_A, ID1, T1), // id<ID4 O | createdAt(T1)>=T2 X → 제외
                    message(ROOM_A, ID2, T2), // id<ID4 O | createdAt(T2)>=T2 O → 포함
                    message(ROOM_A, ID3, T3), // id<ID4 O | createdAt(T3)>=T2 O → 포함
                    message(ROOM_A, ID4, T4), // id<ID4 X (cursor 자체)        → 제외
                    message(ROOM_A, ID5, T5)); // id<ID4 X                      → 제외

            List<ChatMessage> result = chatMessageRepository.findByRoomIdAndIdLessThanAndCreatedAtGreaterThanEqual(
                    ROOM_A, ID4, T2, descById(10));

            assertThat(result).extracting(ChatMessage::getId).containsExactly(ID3, ID2);
        }

        @Test
        @DisplayName("enteredAt 조건을 만족해도 커서 이상이면 제외된다")
        void enteredAt_만족해도_커서_이상_제외() {
            save(
                    message(ROOM_A, ID2, T2), // id<ID3 O | createdAt(T2)>=T2 O → 포함
                    message(ROOM_A, ID3, T3), // id<ID3 X (cursor 자체)         → 제외
                    message(ROOM_A, ID4, T4)); // id<ID3 X                       → 제외

            List<ChatMessage> result = chatMessageRepository.findByRoomIdAndIdLessThanAndCreatedAtGreaterThanEqual(
                    ROOM_A, ID3, T2, descById(10));

            assertThat(result).extracting(ChatMessage::getId).containsExactly(ID2);
        }
    }

    @Nested
    @DisplayName("findByRoomIdAndIdGreaterThanAndCreatedAtGreaterThanEqual — 안읽은 메시지 정방향 조회")
    class FindByRoomIdAndIdGreaterThanAndCreatedAtGreaterThanEqual {

        /** _id 오름차순 PageRequest */
        private PageRequest ascById(int size) {
            return PageRequest.of(0, size, Sort.by(Sort.Direction.ASC, "_id"));
        }

        @Test
        @DisplayName("afterId 초과 메시지만 오래된순으로 반환한다")
        void afterId_초과_메시지_오래된순_반환() {
            save(
                    message(ROOM_A, ID1, T1), // afterId 이하 — 제외
                    message(ROOM_A, ID2, T2), // afterId 자체 — 제외
                    message(ROOM_A, ID3, T3), // 포함
                    message(ROOM_A, ID4, T4)); // 포함

            List<ChatMessage> result = chatMessageRepository.findByRoomIdAndIdGreaterThanAndCreatedAtGreaterThanEqual(
                    ROOM_A, ID2, T1, ascById(10));

            assertThat(result).extracting(ChatMessage::getId).containsExactly(ID3, ID4); // 오래된순(ASC)
        }

        @Test
        @DisplayName("afterId와 동일한 ID는 결과에 포함되지 않는다 (GreaterThan 경계)")
        void afterId_동일_ID_미포함() {
            save(message(ROOM_A, ID2, T2));

            List<ChatMessage> result = chatMessageRepository.findByRoomIdAndIdGreaterThanAndCreatedAtGreaterThanEqual(
                    ROOM_A, ID2, T1, ascById(10));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("enteredAt 미만 메시지는 결과에 포함되지 않는다")
        void enteredAt_미만_메시지_미포함() {
            save(
                    message(ROOM_A, ID2, T2), // afterId(ID1) 초과 O | createdAt(T2) >= enteredAt(T3) X → 제외
                    message(ROOM_A, ID3, T3), // afterId(ID1) 초과 O | createdAt(T3) >= enteredAt(T3) O → 포함
                    message(ROOM_A, ID4, T4)); // afterId(ID1) 초과 O | createdAt(T4) >= enteredAt(T3) O → 포함

            List<ChatMessage> result = chatMessageRepository.findByRoomIdAndIdGreaterThanAndCreatedAtGreaterThanEqual(
                    ROOM_A, ID1, T3, ascById(10));

            assertThat(result).extracting(ChatMessage::getId).containsExactly(ID3, ID4);
        }

        @Test
        @DisplayName("id > afterId AND createdAt >= enteredAt 두 조건을 동시에 만족하는 메시지만 반환한다")
        void 두_조건_동시_만족_메시지만_반환() {
            // afterId=ID2, enteredAt=T3
            save(
                    message(ROOM_A, ID1, T1), // id>ID2 X                              → 제외
                    message(ROOM_A, ID2, T2), // id>ID2 X (afterId 자체)               → 제외
                    message(ROOM_A, ID3, T3), // id>ID2 O | createdAt(T3)>=T3 O        → 포함
                    message(ROOM_A, ID4, T2), // id>ID2 O | createdAt(T2)>=T3 X        → 제외
                    message(ROOM_A, ID5, T5)); // id>ID2 O | createdAt(T5)>=T3 O        → 포함

            List<ChatMessage> result = chatMessageRepository.findByRoomIdAndIdGreaterThanAndCreatedAtGreaterThanEqual(
                    ROOM_A, ID2, T3, ascById(10));

            assertThat(result).extracting(ChatMessage::getId).containsExactly(ID3, ID5);
        }

        @Test
        @DisplayName("다른 방 메시지는 결과에 포함되지 않는다")
        void 다른_방_메시지_미포함() {
            save(
                    message(ROOM_A, ID2, T2), // ROOM_A — 포함
                    message(ROOM_B, ID3, T3)); // ROOM_B — 제외

            List<ChatMessage> result = chatMessageRepository.findByRoomIdAndIdGreaterThanAndCreatedAtGreaterThanEqual(
                    ROOM_A, ID1, T1, ascById(10));

            assertThat(result).hasSize(1).extracting(ChatMessage::getId).containsExactly(ID2);
        }
    }

    @Nested
    @DisplayName("countByRoomId — 전체 메시지 수")
    class CountByRoomId {

        @Test
        @DisplayName("해당 방의 전체 메시지 수를 반환하고 다른 방은 카운트하지 않는다")
        void 해당_방_전체_메시지_수() {
            save(message(ROOM_A, ID1, T1), message(ROOM_A, ID2, T2), message(ROOM_B, ID3, T3)); // 다른 방

            assertThat(chatMessageRepository.countByRoomId(ROOM_A)).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("countByRoomIdAndCreatedAtGreaterThanEqual — enteredAt 이후 메시지 수")
    class CountByRoomIdAndCreatedAtGreaterThanEqual {

        @Test
        @DisplayName("enteredAt 이상의 메시지 수만 카운트한다 (재진입 unread 복구용)")
        void enteredAt_이상_메시지_수_카운트() {
            save(
                    message(ROOM_A, ID1, T1), // 제외
                    message(ROOM_A, ID2, T2), // 포함 (경계)
                    message(ROOM_A, ID3, T3)); // 포함

            assertThat(chatMessageRepository.countByRoomIdAndCreatedAtGreaterThanEqual(ROOM_A, T2))
                    .isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("countByRoomIdAndIdGreaterThan — 안읽은 메시지 수 (Redis 재구축용)")
    class CountByRoomIdAndIdGreaterThan {

        @Test
        @DisplayName("lastReadId 초과 메시지 수를 반환한다")
        void lastReadId_초과_메시지_수() {
            save(
                    message(ROOM_A, ID1, T1), // lastReadId — 포함 안 됨
                    message(ROOM_A, ID2, T2), // 안읽음
                    message(ROOM_A, ID3, T3), // 안읽음
                    message(ROOM_B, ID4, T4)); // 다른 방

            assertThat(chatMessageRepository.countByRoomIdAndIdGreaterThan(ROOM_A, ID1))
                    .isEqualTo(2);
        }

        @Test
        @DisplayName("lastReadId가 최신 메시지이면 0을 반환한다 (모두 읽음)")
        void 최신_메시지_읽은_경우_0() {
            save(message(ROOM_A, ID1, T1), message(ROOM_A, ID2, T2));

            assertThat(chatMessageRepository.countByRoomIdAndIdGreaterThan(ROOM_A, ID2))
                    .isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("deleteAllByRoomId — 채팅방 메시지 삭제")
    class DeleteAllByRoomId {

        @Test
        @DisplayName("해당 방의 메시지만 삭제하고 다른 방 메시지는 유지한다")
        void 해당_방만_삭제_다른_방_유지() {
            save(message(ROOM_A, ID1, T1), message(ROOM_A, ID2, T2), message(ROOM_B, ID3, T3));

            chatMessageRepository.deleteAllByRoomId(ROOM_A);

            assertThat(chatMessageRepository.countByRoomId(ROOM_A)).isEqualTo(0);
            assertThat(chatMessageRepository.countByRoomId(ROOM_B)).isEqualTo(1);
        }
    }
}
