package com.example.kloset_lab.chat.infrastructure;

import com.example.kloset_lab.chat.dto.stomp.ChatBroadcastMessage;
import com.example.kloset_lab.chat.dto.stomp.ChatRoomUpdateEvent;
import com.example.kloset_lab.global.infrastructure.RedisEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 채팅 이벤트 발행자 (RedisEventPublisher에 위임) */
@Component
@RequiredArgsConstructor
public class ChatEventPublisher {

    private static final String ROOM_MSG_CHANNEL = "chat:msg:%d";
    private static final String ROOM_UPDATE_CHANNEL = "chat:update:%d";

    private final RedisEventPublisher redisEventPublisher;

    /**
     * 채팅방 메시지 브로드캐스트 발행
     *
     * @param roomId 채팅방 ID
     * @param message 브로드캐스트 메시지
     */
    public void publishRoomMessage(Long roomId, ChatBroadcastMessage message) {
        redisEventPublisher.publish(String.format(ROOM_MSG_CHANNEL, roomId), message);
    }

    /**
     * 채팅 목록 갱신 이벤트 발행
     *
     * @param userId 대상 사용자 ID
     * @param event 목록 갱신 이벤트
     */
    public void publishRoomUpdate(Long userId, ChatRoomUpdateEvent event) {
        redisEventPublisher.publish(String.format(ROOM_UPDATE_CHANNEL, userId), event);
    }
}
