package com.example.kloset_lab.chat.handler;

import com.example.kloset_lab.chat.constant.ChatConstants;
import com.example.kloset_lab.chat.dto.stomp.ChatErrorMessage;
import com.example.kloset_lab.chat.dto.stomp.ChatSendRequest;
import com.example.kloset_lab.chat.service.ChatMessageService;
import com.example.kloset_lab.global.exception.CustomException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

/** STOMP 메시지 수신 핸들러 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatMessageHandler {

    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 채팅 메시지 전송 처리
     *
     * @param roomId         채팅방 ID (path variable)
     * @param request        메시지 전송 요청
     * @param headerAccessor STOMP 헤더 (세션 속성)
     */
    @MessageMapping("/chat/rooms/{roomId}/messages")
    public void sendMessage(
            @DestinationVariable Long roomId,
            @Payload ChatSendRequest request,
            SimpMessageHeaderAccessor headerAccessor) {

        Long userId = extractUserId(headerAccessor);
        if (userId == null) {
            log.warn("STOMP 메시지 처리 실패: userId 없음 (roomId: {})", roomId);
            return;
        }

        try {
            chatMessageService.sendMessage(userId, roomId, request);
        } catch (CustomException e) {
            log.warn("채팅 메시지 전송 실패 - userId: {}, roomId: {}, error: {}", userId, roomId, e.getMessage());
            sendError(userId, request.clientMessageId(), e.getErrorCode().name(), e.getMessage());
        } catch (Exception e) {
            log.error("채팅 메시지 전송 중 예외 발생 - userId: {}, roomId: {}", userId, roomId, e);
            sendError(userId, request.clientMessageId(), "INTERNAL_SERVER_ERROR", ChatConstants.ERR_MSG_SEND_FAILED);
        }
    }

    private void sendError(Long userId, String clientMessageId, String errorCode, String message) {
        ChatErrorMessage error = ChatErrorMessage.builder()
                .clientMessageId(clientMessageId)
                .errorCode(errorCode)
                .message(message)
                .build();
        messagingTemplate.convertAndSendToUser(String.valueOf(userId), "/queue/errors", error);
    }

    /**
     * 세션 속성에서 userId 추출
     *
     * @param headerAccessor STOMP 헤더
     * @return userId (없으면 null)
     */
    private Long extractUserId(SimpMessageHeaderAccessor headerAccessor) {
        return Optional.ofNullable(headerAccessor.getSessionAttributes())
                .map(attrs -> attrs.get(ChatConstants.SESSION_ATTR_USER_ID))
                .filter(obj -> obj instanceof Long)
                .map(obj -> (Long) obj)
                .orElse(null);
    }
}
