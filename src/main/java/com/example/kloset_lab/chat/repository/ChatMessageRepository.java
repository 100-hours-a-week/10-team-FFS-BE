package com.example.kloset_lab.chat.repository;

import com.example.kloset_lab.chat.document.ChatMessage;
import java.util.List;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ChatMessageRepository extends MongoRepository<ChatMessage, ObjectId> {

    /**
     * 커서 기반 채팅 메시지 조회 (최신순)
     *
     * @param roomId   채팅방 ID
     * @param cursor   이 ID 미만의 메시지만 조회 (커서 페이지네이션)
     * @param pageable 페이징 정보 (sort: _id desc)
     * @return 메시지 목록
     */
    List<ChatMessage> findByRoomIdAndIdLessThan(Long roomId, ObjectId cursor, Pageable pageable);

    /**
     * 첫 페이지 채팅 메시지 조회 (커서 없이)
     *
     * @param roomId   채팅방 ID
     * @param pageable 페이징 정보 (sort: _id desc)
     * @return 메시지 목록
     */
    List<ChatMessage> findByRoomId(Long roomId, Pageable pageable);

    /**
     * 채팅방 전체 메시지 수 조회 (lastReadMessageId가 없는 경우 unread 복구용)
     *
     * @param roomId 채팅방 ID
     * @return 전체 메시지 수
     */
    long countByRoomId(Long roomId);

    /**
     * 안읽은 메시지 수 조회 (Redis 재구축 시 MongoDB 기준 복구용)
     *
     * @param roomId      채팅방 ID
     * @param lastReadId  마지막으로 읽은 메시지 ID
     * @return 안읽은 메시지 수
     */
    long countByRoomIdAndIdGreaterThan(Long roomId, ObjectId lastReadId);

    /**
     * 채팅방의 모든 메시지 삭제
     *
     * @param roomId 채팅방 ID
     */
    void deleteAllByRoomId(Long roomId);
}
