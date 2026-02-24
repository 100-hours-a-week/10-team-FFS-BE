package com.example.kloset_lab.chat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "chat_room")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "room_type", nullable = false, length = 10)
    private RoomType type = RoomType.DM;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "last_message_id", length = 24)
    private String lastMessageId;

    @Column(name = "last_message_content", length = 255)
    private String lastMessageContent;

    @Column(name = "last_message_at")
    private Instant lastMessageAt;

    @Column(name = "last_message_type", length = 10)
    private String lastMessageType;

    @Builder
    private ChatRoom(Long id) {
        this.id = id;
    }

    public static ChatRoom create() {
        return new ChatRoom();
    }

    /**
     * 방 유형을 지정하여 채팅방 생성
     *
     * @param type 채팅방 유형 (DM / GROUP)
     */
    public static ChatRoom create(RoomType type) {
        ChatRoom room = new ChatRoom();
        room.type = type;
        return room;
    }

    /**
     * 마지막 메시지 스냅샷 갱신
     *
     * @param messageId   MongoDB ObjectId 문자열
     * @param content     메시지 내용 (최대 255자)
     * @param messageType 메시지 타입 (TEXT/IMAGE/FEED)
     * @param sentAt      전송 시각
     */
    public void updateLastMessage(String messageId, String content, String messageType, Instant sentAt) {
        this.lastMessageId = messageId;
        this.lastMessageContent = content;
        this.lastMessageType = messageType;
        this.lastMessageAt = sentAt;
    }

    /** soft delete 처리 */
    public void softDelete() {
        this.deletedAt = Instant.now();
    }

    /** MySQL 스냅샷 재동기화 스케줄러에서 MongoDB 메시지가 없을 때 스냅샷 초기화 */
    public void clearLastMessage() {
        this.lastMessageId = null;
        this.lastMessageContent = null;
        this.lastMessageType = null;
        this.lastMessageAt = null;
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }
}
