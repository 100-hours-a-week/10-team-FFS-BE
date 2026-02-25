package com.example.kloset_lab.chat.repository;

import com.example.kloset_lab.chat.entity.ChatParticipant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, Long> {

    /**
     * 현재 활성 참여자 조회 (left_at IS NULL)
     *
     * @param roomId 채팅방 ID
     * @param userId 사용자 ID
     * @return 활성 참여자 (없거나 나간 경우 empty)
     */
    @Query("SELECT cp FROM ChatParticipant cp"
            + " WHERE cp.room.id = :roomId AND cp.user.id = :userId AND cp.leftAt IS NULL")
    Optional<ChatParticipant> findByRoomIdAndUserId(@Param("roomId") Long roomId, @Param("userId") Long userId);

    /**
     * 히스토리 참여자 조회 — 나간 사용자 포함 (재진입 감지용)
     *
     * @param roomId 채팅방 ID
     * @param userId 사용자 ID
     * @return 참여자 레코드 (없으면 empty)
     */
    @Query("SELECT cp FROM ChatParticipant cp WHERE cp.room.id = :roomId AND cp.user.id = :userId")
    Optional<ChatParticipant> findHistoryByRoomIdAndUserId(@Param("roomId") Long roomId, @Param("userId") Long userId);

    /**
     * 채팅방의 활성 참여자 수 조회 (left_at IS NULL)
     *
     * @param roomId 채팅방 ID
     * @return 활성 참여자 수
     */
    @Query("SELECT COUNT(cp) FROM ChatParticipant cp WHERE cp.room.id = :roomId AND cp.leftAt IS NULL")
    long countByRoomId(@Param("roomId") Long roomId);

    /**
     * 채팅방의 모든 참여자 조회 — 나간 사용자 포함 (상대방 표시용)
     *
     * @param roomId 채팅방 ID
     * @return 전체 참여자 목록
     */
    List<ChatParticipant> findByRoomId(Long roomId);

    /**
     * 채팅방의 활성 참여자 조회 — 나간 사용자 제외 (메시지 브로드캐스트용)
     *
     * @param roomId 채팅방 ID
     * @return 활성 참여자 목록
     */
    @Query("SELECT cp FROM ChatParticipant cp WHERE cp.room.id = :roomId AND cp.leftAt IS NULL")
    List<ChatParticipant> findActiveByRoomId(@Param("roomId") Long roomId);

    /**
     * 특정 사용자의 활성 채팅방 참여자 레코드 조회 (left_at IS NULL)
     *
     * @param userId 사용자 ID
     * @return 활성 참여자 레코드 목록
     */
    @Query("SELECT cp FROM ChatParticipant cp WHERE cp.user.id = :userId AND cp.leftAt IS NULL")
    List<ChatParticipant> findByUserId(@Param("userId") Long userId);
}
