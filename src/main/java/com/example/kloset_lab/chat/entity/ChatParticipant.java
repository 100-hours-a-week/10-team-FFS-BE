package com.example.kloset_lab.chat.entity;

import com.example.kloset_lab.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(
        name = "chat_participant",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uq_room_user",
                        columnNames = {"room_id", "user_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ChatParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private ChatRoom room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "entered_at", nullable = false)
    private Instant enteredAt;

    @Column(name = "last_read_message_id", length = 24)
    private String lastReadMessageId;

    @Column(name = "left_at")
    private Instant leftAt;

    @Builder
    private ChatParticipant(ChatRoom room, User user) {
        this.room = room;
        this.user = user;
        this.enteredAt = Instant.now();
    }

    /**
     * 마지막으로 읽은 메시지 ID 갱신
     *
     * @param messageId MongoDB ObjectId 문자열
     */
    public void updateLastReadMessageId(String messageId) {
        this.lastReadMessageId = messageId;
    }

    /** 채팅방 나가기 (soft delete) */
    public void leave() {
        this.leftAt = Instant.now();
    }

    /** 채팅방 재진입 — leftAt 초기화 및 enteredAt 갱신 */
    public void reenter() {
        this.leftAt = null;
        this.enteredAt = Instant.now();
    }
}
