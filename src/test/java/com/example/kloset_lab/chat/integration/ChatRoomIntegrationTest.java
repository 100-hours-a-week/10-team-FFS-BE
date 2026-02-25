package com.example.kloset_lab.chat.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.kloset_lab.chat.document.ChatMessage;
import com.example.kloset_lab.chat.entity.ChatParticipant;
import com.example.kloset_lab.chat.infrastructure.ChatRedisRepository;
import com.example.kloset_lab.chat.repository.ChatMessageRepository;
import com.example.kloset_lab.chat.repository.ChatParticipantRepository;
import com.example.kloset_lab.chat.repository.ChatRoomRepository;
import com.example.kloset_lab.global.base.IntegrationTest;
import com.example.kloset_lab.user.entity.Provider;
import com.example.kloset_lab.user.entity.User;
import com.example.kloset_lab.user.repository.UserRepository;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;

/** 채팅 도메인 통합 테스트 — MySQL, MongoDB, Redis를 실제 컨테이너로 기동하여 레이어 간 연동을 검증한다. */
class ChatRoomIntegrationTest extends IntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ChatParticipantRepository chatParticipantRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private ChatRedisRepository chatRedisRepository;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        // MySQL 정리 (FK 순서: chat_participant → chat_room → user)
        jdbcTemplate.execute("DELETE FROM chat_participant");
        jdbcTemplate.execute("DELETE FROM chat_room");
        jdbcTemplate.execute("DELETE FROM user");

        // MongoDB 정리
        chatMessageRepository.deleteAll();

        // Redis 정리
        stringRedisTemplate.execute((RedisCallback<Void>) conn -> {
            conn.serverCommands().flushDb();
            return null;
        });

        // 테스트 사용자 생성
        user1 = userRepository.save(User.builder()
                .provider(Provider.KAKAO)
                .providerId("integration-user1")
                .build());
        user2 = userRepository.save(User.builder()
                .provider(Provider.KAKAO)
                .providerId("integration-user2")
                .build());
    }

    // ======================== 헬퍼 ========================

    /** user1 → user2 채팅방 생성 후 roomId 반환 */
    private long createRoom() throws Exception {
        MvcResult result = mockMvc.perform(withAuth(
                        post("/api/v2/chat/rooms")
                                .contentType(APPLICATION_JSON)
                                .content("{\"opponentUserId\":" + user2.getId() + "}"),
                        user1.getId()))
                .andReturn();
        return objectMapper
                .readTree(result.getResponse().getContentAsString())
                .get("data")
                .get("roomId")
                .asLong();
    }

    /** MongoDB에 텍스트 메시지 삽입 후 저장된 ChatMessage 반환 */
    private ChatMessage insertMessage(long roomId, Long senderId, String content) {
        return chatMessageRepository.save(ChatMessage.builder()
                .roomId(roomId)
                .senderId(senderId)
                .type("TEXT")
                .content(content)
                .createdAt(Instant.now())
                .build());
    }

    // ======================== 채팅방 생성/조회 ========================

    @Nested
    class CreateOrGetRoom {

        @Test
        void 채팅방_생성_시_MySQL과_Redis에_저장된다() throws Exception {
            mockMvc.perform(withAuth(
                            post("/api/v2/chat/rooms")
                                    .contentType(APPLICATION_JSON)
                                    .content("{\"opponentUserId\":" + user2.getId() + "}"),
                            user1.getId()))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value(201))
                    .andExpect(jsonPath("$.data.roomId").isNumber());

            // MySQL: 채팅방 1개, 참여자 2명 저장 확인
            assertThat(chatRoomRepository.count()).isEqualTo(1);
            assertThat(chatParticipantRepository.findAll()).hasSize(2);

            // Redis: AFTER_COMMIT 이벤트로 두 사용자 모두 목록에 캐시 확인
            long roomId = chatRoomRepository.findAll().get(0).getId();
            assertThat(chatRedisRepository.getRoomCount(user1.getId())).isEqualTo(1);
            assertThat(chatRedisRepository.getRoomCount(user2.getId())).isEqualTo(1);
            assertThat(chatRedisRepository.getRoomScore(user1.getId(), roomId)).isNotNull();
        }

        @Test
        void 동일_상대방으로_재요청_시_기존_방이_반환된다() throws Exception {
            // 첫 번째 요청 → 201 Created
            MvcResult firstResult = mockMvc.perform(withAuth(
                            post("/api/v2/chat/rooms")
                                    .contentType(APPLICATION_JSON)
                                    .content("{\"opponentUserId\":" + user2.getId() + "}"),
                            user1.getId()))
                    .andExpect(status().isCreated())
                    .andReturn();
            long firstRoomId = objectMapper
                    .readTree(firstResult.getResponse().getContentAsString())
                    .get("data")
                    .get("roomId")
                    .asLong();

            // 두 번째 요청 → 200 OK, 동일 roomId
            mockMvc.perform(withAuth(
                            post("/api/v2/chat/rooms")
                                    .contentType(APPLICATION_JSON)
                                    .content("{\"opponentUserId\":" + user2.getId() + "}"),
                            user1.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.roomId").value(firstRoomId));

            // MySQL: 채팅방 중복 생성 없음
            assertThat(chatRoomRepository.count()).isEqualTo(1);
        }
    }

    // ======================== 메시지 역방향 조회 ========================

    @Nested
    class GetMessages {

        @Test
        void 메시지가_없으면_빈_목록이_반환된다() throws Exception {
            long roomId = createRoom();

            mockMvc.perform(withAuth(get("/api/v2/chat/rooms/{roomId}/messages", roomId), user1.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.messages").isArray())
                    .andExpect(jsonPath("$.data.messages.length()").value(0))
                    .andExpect(jsonPath("$.data.hasNextPage").value(false))
                    .andExpect(jsonPath("$.data.nextCursor").doesNotExist());
        }

        @Test
        void cursor_기반으로_DESC_페이지네이션된다() throws Exception {
            long roomId = createRoom();

            // 3개 메시지 삽입 (삽입 순서 = ObjectId 오름차순 = msg1 < msg2 < msg3)
            insertMessage(roomId, user2.getId(), "message1");
            insertMessage(roomId, user2.getId(), "message2");
            insertMessage(roomId, user2.getId(), "message3");

            // 첫 페이지 (cursor 없음, limit=2) → 최신 2개 DESC 반환
            MvcResult firstPage = mockMvc.perform(withAuth(
                            get("/api/v2/chat/rooms/{roomId}/messages", roomId).param("limit", "2"), user1.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.messages.length()").value(2))
                    .andExpect(jsonPath("$.data.messages[0].content").value("message3"))
                    .andExpect(jsonPath("$.data.messages[1].content").value("message2"))
                    .andExpect(jsonPath("$.data.hasNextPage").value(true))
                    .andExpect(jsonPath("$.data.nextCursor").isString())
                    .andReturn();

            String nextCursor = objectMapper
                    .readTree(firstPage.getResponse().getContentAsString())
                    .get("data")
                    .get("nextCursor")
                    .asText();

            // 두 번째 페이지 (cursor=nextCursor, limit=2) → 나머지 1개 반환
            mockMvc.perform(withAuth(
                            get("/api/v2/chat/rooms/{roomId}/messages", roomId)
                                    .param("cursor", nextCursor)
                                    .param("limit", "2"),
                            user1.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.messages.length()").value(1))
                    .andExpect(jsonPath("$.data.messages[0].content").value("message1"))
                    .andExpect(jsonPath("$.data.hasNextPage").value(false));
        }
    }

    // ======================== 안읽은 메시지 정방향 조회 ========================

    @Nested
    class GetUnreadMessages {

        @Test
        void 메시지가_ASC_순서로_반환되고_읽음_처리된다() throws Exception {
            long roomId = createRoom();

            // 2개 메시지 삽입 (msg1 < msg2 ObjectId 순)
            insertMessage(roomId, user2.getId(), "first");
            insertMessage(roomId, user2.getId(), "second");

            // GET /unread → ASC 순서(오래된 것 먼저)로 반환
            mockMvc.perform(withAuth(get("/api/v2/chat/rooms/{roomId}/messages/unread", roomId), user1.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.messages.length()").value(2))
                    .andExpect(jsonPath("$.data.messages[0].content").value("first"))
                    .andExpect(jsonPath("$.data.messages[1].content").value("second"))
                    .andExpect(jsonPath("$.data.hasNextPage").value(false));

            // MySQL: lastReadMessageId가 가장 최신 메시지(second)로 갱신됨
            ChatParticipant participant = chatParticipantRepository
                    .findHistoryByRoomIdAndUserId(roomId, user1.getId())
                    .orElseThrow();
            assertThat(participant.getLastReadMessageId()).isNotNull();

            // Redis: AFTER_COMMIT 이벤트로 unread count 0 초기화
            assertThat(chatRedisRepository.getUnread(user1.getId(), roomId)).isEqualTo(0);
        }
    }

    // ======================== 읽음 처리 ========================

    @Nested
    class MarkAsRead {

        @Test
        void lastReadMessageId가_갱신되고_Redis_unread가_초기화된다() throws Exception {
            long roomId = createRoom();
            ChatMessage message = insertMessage(roomId, user2.getId(), "읽을 메시지");
            String messageId = message.getId().toHexString();

            // 인위적으로 unread 카운트 증가 (실제 메시지 수신 시나리오 재현)
            chatRedisRepository.incrementUnread(user1.getId(), roomId);
            assertThat(chatRedisRepository.getUnread(user1.getId(), roomId)).isEqualTo(1);

            // PUT /read
            mockMvc.perform(withAuth(
                            put("/api/v2/chat/rooms/{roomId}/read", roomId)
                                    .contentType(APPLICATION_JSON)
                                    .content("{\"lastReadMessageId\":\"" + messageId + "\"}"),
                            user1.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));

            // MySQL: lastReadMessageId 갱신 확인
            ChatParticipant participant = chatParticipantRepository
                    .findHistoryByRoomIdAndUserId(roomId, user1.getId())
                    .orElseThrow();
            assertThat(participant.getLastReadMessageId()).isEqualTo(messageId);

            // Redis: AFTER_COMMIT 이벤트로 unread count 0 초기화
            assertThat(chatRedisRepository.getUnread(user1.getId(), roomId)).isEqualTo(0);
        }
    }

    // ======================== 채팅방 나가기 ========================

    @Nested
    class LeaveRoom {

        @Test
        void leftAt이_설정되고_Redis에서_방이_제거된다() throws Exception {
            long roomId = createRoom();

            // 방 생성 후 user1의 Redis 캐시 존재 확인
            assertThat(chatRedisRepository.getRoomCount(user1.getId())).isEqualTo(1);

            // DELETE /participants
            mockMvc.perform(withAuth(delete("/api/v2/chat/rooms/{roomId}/participants", roomId), user1.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));

            // MySQL: user1 참여자에 leftAt 설정 확인 (findHistoryByRoomIdAndUserId는 leftAt 무관하게 조회)
            ChatParticipant participant = chatParticipantRepository
                    .findHistoryByRoomIdAndUserId(roomId, user1.getId())
                    .orElseThrow();
            assertThat(participant.getLeftAt()).isNotNull();

            // Redis: AFTER_COMMIT 이벤트로 user1의 채팅방 ZSet에서 제거
            assertThat(chatRedisRepository.getRoomCount(user1.getId())).isEqualTo(0);
        }
    }
}
