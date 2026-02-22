package com.example.kloset_lab.chat.entity;

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
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "chat_participant")
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

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "entered_at", nullable = false)
    private Instant enteredAt;

    @Column(name = "last_read_message_id", length = 24)
    private String lastReadMessageId;

    @Builder
    private ChatParticipant(ChatRoom room, Long userId) {
        this.room = room;
        this.userId = userId;
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
}
