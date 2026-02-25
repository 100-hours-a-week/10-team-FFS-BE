package com.example.kloset_lab.chat.repository;

import com.example.kloset_lab.chat.document.ChatMessage;
import java.time.Instant;
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
     * 커서 + enteredAt 기반 채팅 메시지 조회 (재진입 사용자용)
     *
     * @param roomId    채팅방 ID
     * @param cursor    이 ID 미만의 메시지만 조회
     * @param enteredAt 이 시각 이후 메시지만 조회 (입장 시각)
     * @param pageable  페이징 정보 (sort: _id desc)
     * @return 메시지 목록
     */
    List<ChatMessage> findByRoomIdAndIdLessThanAndCreatedAtGreaterThanEqual(
            Long roomId, ObjectId cursor, Instant enteredAt, Pageable pageable);

    /**
     * 첫 페이지 채팅 메시지 조회 (커서 없이)
     *
     * @param roomId   채팅방 ID
     * @param pageable 페이징 정보 (sort: _id desc)
     * @return 메시지 목록
     */
    List<ChatMessage> findByRoomId(Long roomId, Pageable pageable);

    /**
     * 첫 페이지 채팅 메시지 조회 — enteredAt 기준 필터 (재진입 사용자용)
     *
     * @param roomId    채팅방 ID
     * @param enteredAt 이 시각 이후 메시지만 조회 (입장 시각)
     * @param pageable  페이징 정보 (sort: _id desc)
     * @return 메시지 목록
     */
    List<ChatMessage> findByRoomIdAndCreatedAtGreaterThanEqual(Long roomId, Instant enteredAt, Pageable pageable);

    /**
     * 안읽은 메시지 정방향 조회 (오래된순, cursor 이후 메시지)
     *
     * @param roomId    채팅방 ID
     * @param afterId   이 ID 초과의 메시지만 조회 (커서 페이지네이션)
     * @param enteredAt 이 시각 이후 메시지만 조회 (입장 시각)
     * @param pageable  페이징 정보 (sort: _id asc)
     * @return 메시지 목록
     */
    List<ChatMessage> findByRoomIdAndIdGreaterThanAndCreatedAtGreaterThanEqual(
            Long roomId, ObjectId afterId, Instant enteredAt, Pageable pageable);

    /**
     * 채팅방 전체 메시지 수 조회 (lastReadMessageId가 없는 경우 unread 복구용)
     *
     * @param roomId 채팅방 ID
     * @return 전체 메시지 수
     */
    long countByRoomId(Long roomId);

    /**
     * enteredAt 이후 전체 메시지 수 조회 (재진입 사용자 unread 복구용)
     *
     * @param roomId    채팅방 ID
     * @param enteredAt 이 시각 이후 메시지만 카운트
     * @return 메시지 수
     */
    long countByRoomIdAndCreatedAtGreaterThanEqual(Long roomId, Instant enteredAt);

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
