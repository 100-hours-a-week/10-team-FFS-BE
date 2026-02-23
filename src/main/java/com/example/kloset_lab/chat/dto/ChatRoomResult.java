package com.example.kloset_lab.chat.dto;

/** 채팅방 생성 또는 기존 방 반환 결과 */
public record ChatRoomResult(ChatRoomResponse room, boolean created) {

    public static ChatRoomResult created(ChatRoomResponse room) {
        return new ChatRoomResult(room, true);
    }

    public static ChatRoomResult existing(ChatRoomResponse room) {
        return new ChatRoomResult(room, false);
    }
}
