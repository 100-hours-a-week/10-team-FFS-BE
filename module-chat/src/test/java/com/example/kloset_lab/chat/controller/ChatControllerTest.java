package com.example.kloset_lab.chat.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.kloset_lab.chat.dto.ChatMessageItem;
import com.example.kloset_lab.chat.dto.ChatMessageListResponse;
import com.example.kloset_lab.chat.dto.ChatRoomCreateRequest;
import com.example.kloset_lab.chat.dto.ChatRoomListResponse;
import com.example.kloset_lab.chat.dto.ChatRoomResponse;
import com.example.kloset_lab.chat.dto.ChatRoomResult;
import com.example.kloset_lab.chat.dto.ReadRequest;
import com.example.kloset_lab.chat.dto.UnreadStatusResponse;
import com.example.kloset_lab.chat.service.ChatRoomService;
import com.example.kloset_lab.global.base.ChatServerControllerTest;
import com.example.kloset_lab.global.exception.CustomException;
import com.example.kloset_lab.global.exception.ErrorCode;
import com.example.kloset_lab.global.response.Message;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(ChatController.class)
@DisplayName("ChatController 슬라이스 테스트")
class ChatControllerTest extends ChatServerControllerTest {

    @MockitoBean
    private ChatRoomService chatRoomService;

    // ──────────────────────────────────────────────────────────────────────────
    // POST /api/v2/chat/rooms — 채팅방 생성 또는 기존 방 조회
    // ──────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("POST /api/v2/chat/rooms")
    class CreateOrGetRoom {

        @Test
        @DisplayName("인증 없이 요청하면 401을 반환한다")
        void 인증_없으면_401() throws Exception {
            // @WebMvcTest는 SecurityConfig를 로드하지 않아 기본 Spring Security의 CSRF가 활성화된다.
            // csrf()를 포함해야 CSRF 403을 우회하고 순수 인증 실패(401)를 테스트할 수 있다.
            mockMvc.perform(post("/api/v2/chat/rooms")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"opponentUserId\":2}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("opponentUserId가 null이면 400을 반환한다")
        void opponentUserId_null_이면_400() throws Exception {
            mockMvc.perform(withAuth(post("/api/v2/chat/rooms")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400));
        }

        @Test
        @DisplayName("기존 채팅방이 있으면 200을 반환한다")
        void 기존_채팅방_200() throws Exception {
            ChatRoomResponse room = ChatRoomResponse.builder()
                    .roomId(10L)
                    .opponent(null)
                    .createdAt(Instant.now())
                    .build();
            given(chatRoomService.createOrGetRoom(anyLong(), any(ChatRoomCreateRequest.class)))
                    .willReturn(ChatRoomResult.existing(room));

            mockMvc.perform(withAuth(post("/api/v2/chat/rooms")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"opponentUserId\":2}")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value(Message.CHAT_ROOM_RETRIEVED))
                    .andExpect(jsonPath("$.data.roomId").value(10));
        }

        @Test
        @DisplayName("새 채팅방을 생성하면 201을 반환한다")
        void 새_채팅방_생성_201() throws Exception {
            ChatRoomResponse room = ChatRoomResponse.builder()
                    .roomId(11L)
                    .opponent(null)
                    .createdAt(Instant.now())
                    .build();
            given(chatRoomService.createOrGetRoom(anyLong(), any(ChatRoomCreateRequest.class)))
                    .willReturn(ChatRoomResult.created(room));

            mockMvc.perform(withAuth(post("/api/v2/chat/rooms")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"opponentUserId\":2}")))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value(201))
                    .andExpect(jsonPath("$.message").value(Message.CHAT_ROOM_CREATED))
                    .andExpect(jsonPath("$.data.roomId").value(11));
        }

        @Test
        @DisplayName("서비스에서 CANNOT_CHAT_WITH_SELF가 발생하면 400을 반환한다")
        void 서비스_예외_400() throws Exception {
            given(chatRoomService.createOrGetRoom(anyLong(), any(ChatRoomCreateRequest.class)))
                    .willThrow(new CustomException(ErrorCode.CANNOT_CHAT_WITH_SELF));

            mockMvc.perform(withAuth(post("/api/v2/chat/rooms")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"opponentUserId\":2}")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.message").value(ErrorCode.CANNOT_CHAT_WITH_SELF.getMessage()));
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // GET /api/v2/chat/rooms — 채팅방 목록 조회
    // ──────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("GET /api/v2/chat/rooms")
    class GetRooms {

        @Test
        @DisplayName("인증 없이 요청하면 401을 반환한다")
        void 인증_없으면_401() throws Exception {
            mockMvc.perform(get("/api/v2/chat/rooms")).andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("채팅방 목록을 200으로 반환한다")
        void 채팅방_목록_200() throws Exception {
            ChatRoomListResponse response = ChatRoomListResponse.builder()
                    .rooms(List.of())
                    .hasNextPage(false)
                    .nextCursor(null)
                    .build();
            given(chatRoomService.getRooms(anyLong(), any(), anyInt())).willReturn(response);

            mockMvc.perform(withAuth(get("/api/v2/chat/rooms")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value(Message.CHAT_ROOMS_RETRIEVED))
                    .andExpect(jsonPath("$.data.rooms").isArray())
                    .andExpect(jsonPath("$.data.hasNextPage").value(false));
        }

        @Test
        @DisplayName("limit 파라미터 없이 요청하면 기본값 20이 서비스에 전달된다")
        void 기본_limit_20_전달() throws Exception {
            given(chatRoomService.getRooms(eq(TEST_USER_ID), isNull(), eq(20)))
                    .willReturn(ChatRoomListResponse.builder()
                            .rooms(List.of())
                            .hasNextPage(false)
                            .nextCursor(null)
                            .build());

            mockMvc.perform(withAuth(get("/api/v2/chat/rooms"))).andExpect(status().isOk());

            then(chatRoomService).should().getRooms(TEST_USER_ID, null, 20);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // GET /api/v2/chat/rooms/{roomId}/messages — 메시지 목록 조회
    // ──────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("GET /api/v2/chat/rooms/{roomId}/messages")
    class GetMessages {

        @Test
        @DisplayName("인증 없이 요청하면 401을 반환한다")
        void 인증_없으면_401() throws Exception {
            mockMvc.perform(get("/api/v2/chat/rooms/1/messages")).andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("메시지 목록을 200으로 반환한다")
        void 메시지_목록_200() throws Exception {
            ChatMessageListResponse response = ChatMessageListResponse.builder()
                    .messages(List.of())
                    .hasNextPage(false)
                    .nextCursor(null)
                    .build();
            given(chatRoomService.getMessages(anyLong(), anyLong(), isNull(), anyInt()))
                    .willReturn(response);

            mockMvc.perform(withAuth(get("/api/v2/chat/rooms/1/messages")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value(Message.CHAT_MESSAGES_RETRIEVED))
                    .andExpect(jsonPath("$.data.messages").isArray())
                    .andExpect(jsonPath("$.data.hasNextPage").value(false));
        }

        @Test
        @DisplayName("cursor 파라미터를 전달하면 서비스에 cursor가 전달된다")
        void cursor_파라미터_전달() throws Exception {
            String cursor = "507f1f77bcf86cd799439011";
            ChatMessageListResponse response = ChatMessageListResponse.builder()
                    .messages(List.of())
                    .hasNextPage(false)
                    .nextCursor(null)
                    .build();
            given(chatRoomService.getMessages(anyLong(), anyLong(), eq(cursor), anyInt()))
                    .willReturn(response);

            mockMvc.perform(withAuth(get("/api/v2/chat/rooms/1/messages").param("cursor", cursor)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test
        @DisplayName("서비스에서 CHAT_ROOM_ACCESS_DENIED가 발생하면 403을 반환한다")
        void 서비스_예외_403() throws Exception {
            given(chatRoomService.getMessages(anyLong(), anyLong(), isNull(), anyInt()))
                    .willThrow(new CustomException(ErrorCode.CHAT_ROOM_ACCESS_DENIED));

            mockMvc.perform(withAuth(get("/api/v2/chat/rooms/1/messages")))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(403))
                    .andExpect(jsonPath("$.message").value(ErrorCode.CHAT_ROOM_ACCESS_DENIED.getMessage()));
        }

        @Test
        @DisplayName("limit 파라미터 없이 요청하면 기본값 50이 서비스에 전달된다")
        void 기본_limit_50_전달() throws Exception {
            given(chatRoomService.getMessages(eq(TEST_USER_ID), eq(1L), isNull(), eq(50)))
                    .willReturn(ChatMessageListResponse.builder()
                            .messages(List.of())
                            .hasNextPage(false)
                            .nextCursor(null)
                            .build());

            mockMvc.perform(withAuth(get("/api/v2/chat/rooms/1/messages"))).andExpect(status().isOk());

            then(chatRoomService).should().getMessages(TEST_USER_ID, 1L, null, 50);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // GET /api/v2/chat/rooms/{roomId}/messages/unread — 안읽은 메시지 조회
    // ──────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("GET /api/v2/chat/rooms/{roomId}/messages/unread")
    class GetUnreadMessages {

        @Test
        @DisplayName("인증 없이 요청하면 401을 반환한다")
        void 인증_없으면_401() throws Exception {
            mockMvc.perform(get("/api/v2/chat/rooms/1/messages/unread")).andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("cursor 없이 요청하면 200으로 안읽은 메시지를 반환한다")
        void cursor_없이_200() throws Exception {
            ChatMessageItem item = ChatMessageItem.builder()
                    .messageId("507f1f77bcf86cd799439011")
                    .senderId(2L)
                    .build();
            ChatMessageListResponse response = ChatMessageListResponse.builder()
                    .messages(List.of(item))
                    .hasNextPage(false)
                    .nextCursor(null)
                    .build();
            given(chatRoomService.getUnreadMessages(anyLong(), anyLong(), isNull(), anyInt()))
                    .willReturn(response);

            mockMvc.perform(withAuth(get("/api/v2/chat/rooms/1/messages/unread")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value(Message.CHAT_UNREAD_MESSAGES_RETRIEVED))
                    .andExpect(jsonPath("$.data.messages").isArray())
                    .andExpect(jsonPath("$.data.messages[0].messageId").value("507f1f77bcf86cd799439011"))
                    .andExpect(jsonPath("$.data.hasNextPage").value(false))
                    .andExpect(jsonPath("$.data.nextCursor").doesNotExist());
        }

        @Test
        @DisplayName("cursor를 전달하면 서비스에 cursor가 전달된다")
        void cursor_전달_200() throws Exception {
            String cursor = "507f1f77bcf86cd799439011";
            String nextCursor = "507f1f77bcf86cd799439022";
            ChatMessageListResponse response = ChatMessageListResponse.builder()
                    .messages(List.of())
                    .hasNextPage(true)
                    .nextCursor(nextCursor)
                    .build();
            given(chatRoomService.getUnreadMessages(anyLong(), anyLong(), eq(cursor), anyInt()))
                    .willReturn(response);

            mockMvc.perform(withAuth(get("/api/v2/chat/rooms/1/messages/unread").param("cursor", cursor)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.hasNextPage").value(true))
                    .andExpect(jsonPath("$.data.nextCursor").value(nextCursor));
        }

        @Test
        @DisplayName("서비스에서 CHAT_ROOM_ACCESS_DENIED가 발생하면 403을 반환한다")
        void 서비스_예외_403() throws Exception {
            given(chatRoomService.getUnreadMessages(anyLong(), anyLong(), isNull(), anyInt()))
                    .willThrow(new CustomException(ErrorCode.CHAT_ROOM_ACCESS_DENIED));

            mockMvc.perform(withAuth(get("/api/v2/chat/rooms/1/messages/unread")))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(403))
                    .andExpect(jsonPath("$.message").value(ErrorCode.CHAT_ROOM_ACCESS_DENIED.getMessage()));
        }

        @Test
        @DisplayName("limit 파라미터 없이 요청하면 기본값 50이 서비스에 전달된다")
        void 기본_limit_50_전달() throws Exception {
            given(chatRoomService.getUnreadMessages(eq(TEST_USER_ID), eq(1L), isNull(), eq(50)))
                    .willReturn(ChatMessageListResponse.builder()
                            .messages(List.of())
                            .hasNextPage(false)
                            .nextCursor(null)
                            .build());

            mockMvc.perform(withAuth(get("/api/v2/chat/rooms/1/messages/unread")))
                    .andExpect(status().isOk());

            then(chatRoomService).should().getUnreadMessages(TEST_USER_ID, 1L, null, 50);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // DELETE /api/v2/chat/rooms/{roomId}/participants — 채팅방 나가기
    // ──────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("DELETE /api/v2/chat/rooms/{roomId}/participants")
    class LeaveRoom {

        @Test
        @DisplayName("인증 없이 요청하면 401을 반환한다")
        void 인증_없으면_401() throws Exception {
            mockMvc.perform(delete("/api/v2/chat/rooms/1/participants").with(csrf()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("채팅방을 나가면 200을 반환한다")
        void 채팅방_나가기_200() throws Exception {
            willDoNothing().given(chatRoomService).leaveRoom(anyLong(), anyLong());

            mockMvc.perform(withAuth(delete("/api/v2/chat/rooms/1/participants")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value(Message.CHAT_ROOM_LEFT));
        }

        @Test
        @DisplayName("서비스에서 CHAT_ROOM_ACCESS_DENIED가 발생하면 403을 반환한다")
        void 서비스_예외_403() throws Exception {
            willThrow(new CustomException(ErrorCode.CHAT_ROOM_ACCESS_DENIED))
                    .given(chatRoomService)
                    .leaveRoom(anyLong(), anyLong());

            mockMvc.perform(withAuth(delete("/api/v2/chat/rooms/1/participants")))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(403))
                    .andExpect(jsonPath("$.message").value(ErrorCode.CHAT_ROOM_ACCESS_DENIED.getMessage()));
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // PUT /api/v2/chat/rooms/{roomId}/read — 메시지 읽음 처리
    // ──────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("PUT /api/v2/chat/rooms/{roomId}/read")
    class MarkAsRead {

        @Test
        @DisplayName("인증 없이 요청하면 401을 반환한다")
        void 인증_없으면_401() throws Exception {
            mockMvc.perform(put("/api/v2/chat/rooms/1/read")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"lastReadMessageId\":\"507f1f77bcf86cd799439011\"}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("lastReadMessageId가 blank이면 400을 반환한다")
        void lastReadMessageId_blank_이면_400() throws Exception {
            mockMvc.perform(withAuth(put("/api/v2/chat/rooms/1/read")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"lastReadMessageId\":\"\"}")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400));
        }

        @Test
        @DisplayName("서비스에서 INVALID_REQUEST가 발생하면 400을 반환한다")
        void 서비스_예외_400() throws Exception {
            willThrow(new CustomException(ErrorCode.INVALID_REQUEST))
                    .given(chatRoomService)
                    .markAsRead(anyLong(), anyLong(), any(ReadRequest.class));

            mockMvc.perform(withAuth(put("/api/v2/chat/rooms/1/read")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"lastReadMessageId\":\"not-an-objectid\"}")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.message").value(ErrorCode.INVALID_REQUEST.getMessage()));
        }

        @Test
        @DisplayName("읽음 처리 성공 시 200을 반환한다")
        void 읽음_처리_200() throws Exception {
            willDoNothing().given(chatRoomService).markAsRead(anyLong(), anyLong(), any(ReadRequest.class));

            mockMvc.perform(withAuth(put("/api/v2/chat/rooms/1/read")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"lastReadMessageId\":\"507f1f77bcf86cd799439011\"}")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value(Message.CHAT_MARKED_AS_READ));
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // GET /api/v2/chat/unread — 전체 안읽은 메시지 현황 조회
    // ──────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("GET /api/v2/chat/unread")
    class GetUnreadStatus {

        @Test
        @DisplayName("인증 없이 요청하면 401을 반환한다")
        void 인증_없으면_401() throws Exception {
            mockMvc.perform(get("/api/v2/chat/unread")).andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("안읽은 메시지 현황을 200으로 반환한다")
        void 안읽은_현황_200() throws Exception {
            UnreadStatusResponse response = UnreadStatusResponse.builder()
                    .hasUnread(true)
                    .totalUnreadCount(5L)
                    .build();
            given(chatRoomService.getUnreadStatus(anyLong())).willReturn(response);

            mockMvc.perform(withAuth(get("/api/v2/chat/unread")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value(Message.CHAT_UNREAD_STATUS_RETRIEVED))
                    .andExpect(jsonPath("$.data.hasUnread").value(true))
                    .andExpect(jsonPath("$.data.totalUnreadCount").value(5));
        }
    }
}
