package com.example.kloset_lab.chat.controller;

import com.example.kloset_lab.chat.dto.ChatMessageListResponse;
import com.example.kloset_lab.chat.dto.ChatRoomCreateRequest;
import com.example.kloset_lab.chat.dto.ChatRoomListResponse;
import com.example.kloset_lab.chat.dto.ChatRoomResponse;
import com.example.kloset_lab.chat.dto.ChatRoomResult;
import com.example.kloset_lab.chat.dto.ReadRequest;
import com.example.kloset_lab.chat.dto.UnreadStatusResponse;
import com.example.kloset_lab.chat.service.ChatRoomService;
import com.example.kloset_lab.global.response.ApiResponse;
import com.example.kloset_lab.global.response.ApiResponses;
import com.example.kloset_lab.global.response.Message;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatRoomService chatRoomService;

    /**
     * 채팅방 생성 또는 기존 방 조회
     *
     * @param userId  현재 로그인한 사용자 ID
     * @param request 채팅방 생성 요청 (opponentUserId)
     * @return 채팅방 응답
     */
    @PostMapping("/rooms")
    public ResponseEntity<ApiResponse<ChatRoomResponse>> createOrGetRoom(
            @AuthenticationPrincipal Long userId, @Valid @RequestBody ChatRoomCreateRequest request) {
        ChatRoomResult result = chatRoomService.createOrGetRoom(userId, request);
        return result.created()
                ? ApiResponses.created(Message.CHAT_ROOM_CREATED, result.room())
                : ApiResponses.ok(Message.CHAT_ROOM_RETRIEVED, result.room());
    }

    /**
     * 채팅방 목록 조회
     *
     * @param userId 현재 로그인한 사용자 ID
     * @param cursor 커서 (이전 페이지 마지막 lastMessageAt 밀리초)
     * @param limit  조회 개수
     * @return 채팅방 목록
     */
    @GetMapping("/rooms")
    public ResponseEntity<ApiResponse<ChatRoomListResponse>> getRooms(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Double cursor,
            @RequestParam(defaultValue = "20") int limit) {
        ChatRoomListResponse response = chatRoomService.getRooms(userId, cursor, limit);
        return ApiResponses.ok(Message.CHAT_ROOMS_RETRIEVED, response);
    }

    /**
     * 채팅 메시지 목록 조회
     *
     * @param userId 현재 로그인한 사용자 ID
     * @param roomId 채팅방 ID
     * @param cursor 커서 (이전 페이지 마지막 메시지 ObjectId)
     * @param limit  조회 개수
     * @return 메시지 목록
     */
    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<ApiResponse<ChatMessageListResponse>> getMessages(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long roomId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "50") int limit) {
        ChatMessageListResponse response = chatRoomService.getMessages(userId, roomId, cursor, limit);
        return ApiResponses.ok(Message.CHAT_MESSAGES_RETRIEVED, response);
    }

    /**
     * 안읽은 메시지 조회 (정방향 페이지네이션, 오래된순→최신순)
     *
     * @param userId 현재 로그인한 사용자 ID
     * @param roomId 채팅방 ID
     * @param cursor 이전 페이지 마지막 메시지 ObjectId. null이면 lastReadMessageId 기준
     * @param limit  조회 개수
     * @return 메시지 목록 (오래된순)
     */
    @GetMapping("/rooms/{roomId}/messages/unread")
    public ResponseEntity<ApiResponse<ChatMessageListResponse>> getUnreadMessages(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long roomId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "50") int limit) {
        ChatMessageListResponse response = chatRoomService.getUnreadMessages(userId, roomId, cursor, limit);
        return ApiResponses.ok(Message.CHAT_UNREAD_MESSAGES_RETRIEVED, response);
    }

    /**
     * 채팅방 나가기
     *
     * @param userId 현재 로그인한 사용자 ID
     * @param roomId 채팅방 ID
     * @return 성공 응답
     */
    @DeleteMapping("/rooms/{roomId}/participants")
    public ResponseEntity<ApiResponse<Void>> leaveRoom(
            @AuthenticationPrincipal Long userId, @PathVariable Long roomId) {
        chatRoomService.leaveRoom(userId, roomId);
        return ApiResponses.ok(Message.CHAT_ROOM_LEFT);
    }

    /**
     * 메시지 읽음 처리
     *
     * @param userId  현재 로그인한 사용자 ID
     * @param roomId  채팅방 ID
     * @param request 읽음 처리 요청
     * @return 성공 응답
     */
    @PutMapping("/rooms/{roomId}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @AuthenticationPrincipal Long userId, @PathVariable Long roomId, @Valid @RequestBody ReadRequest request) {
        chatRoomService.markAsRead(userId, roomId, request);
        return ApiResponses.ok(Message.CHAT_MARKED_AS_READ);
    }

    /**
     * 전체 안읽은 메시지 현황 조회
     *
     * @param userId 현재 로그인한 사용자 ID
     * @return 안읽은 메시지 현황
     */
    @GetMapping("/unread")
    public ResponseEntity<ApiResponse<UnreadStatusResponse>> getUnreadStatus(@AuthenticationPrincipal Long userId) {
        UnreadStatusResponse response = chatRoomService.getUnreadStatus(userId);
        return ApiResponses.ok(Message.CHAT_UNREAD_STATUS_RETRIEVED, response);
    }
}
