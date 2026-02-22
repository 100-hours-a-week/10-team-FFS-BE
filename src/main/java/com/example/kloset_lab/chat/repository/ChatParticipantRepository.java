package com.example.kloset_lab.chat.repository;

import com.example.kloset_lab.chat.entity.ChatParticipant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, Long> {

    /**
     * 채팅방과 사용자 ID로 참여자 조회
     *
     * @param roomId 채팅방 ID
     * @param userId 사용자 ID
     * @return 채팅 참여자 (없으면 empty)
     */
    Optional<ChatParticipant> findByRoomIdAndUserId(Long roomId, Long userId);

    /**
     * 채팅방의 참여자 수 조회
     *
     * @param roomId 채팅방 ID
     * @return 참여자 수
     */
    long countByRoomId(Long roomId);

    /**
     * 채팅방에서 특정 사용자 참여자 레코드 삭제 (hard delete)
     *
     * @param roomId 채팅방 ID
     * @param userId 사용자 ID
     */
    void deleteByRoomIdAndUserId(Long roomId, Long userId);

    /**
     * 채팅방의 모든 참여자 조회
     *
     * @param roomId 채팅방 ID
     * @return 참여자 목록
     */
    List<ChatParticipant> findByRoomId(Long roomId);

    /**
     * 특정 사용자의 채팅방 ID 목록 조회
     *
     * @param userId 사용자 ID
     * @return 채팅방 ID 목록
     */
    List<ChatParticipant> findByUserId(Long userId);
}
