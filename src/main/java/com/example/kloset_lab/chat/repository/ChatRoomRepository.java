package com.example.kloset_lab.chat.repository;

import com.example.kloset_lab.chat.entity.ChatRoom;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    /**
     * 두 유저 사이에 이미 존재하는 채팅방 조회 (삭제되지 않은 방)
     *
     * @param userId         현재 사용자 ID
     * @param opponentUserId 상대방 사용자 ID
     * @return 채팅방 (없으면 empty)
     */
    @Query(
            """
            SELECT cr FROM ChatRoom cr
            WHERE cr.deletedAt IS NULL
              AND EXISTS (
                SELECT 1 FROM ChatParticipant cp1
                WHERE cp1.room = cr AND cp1.userId = :opponentUserId
              )
              AND NOT EXISTS (
                SELECT 1 FROM ChatParticipant cp2
                WHERE cp2.room = cr
                  AND cp2.userId <> :opponentUserId
                  AND cp2.userId <> :userId
              )
            """)
    Optional<ChatRoom> findExistingRoomBetweenUsers(
            @Param("userId") Long userId, @Param("opponentUserId") Long opponentUserId);

    /** soft delete된 채팅방 전체 조회 (고아 메시지 정리 스케줄러용) */
    List<ChatRoom> findAllByDeletedAtIsNotNull();

    /** 활성 채팅방 전체 조회 (MySQL 스냅샷 재동기화 스케줄러용) */
    List<ChatRoom> findAllByDeletedAtIsNull();
}
