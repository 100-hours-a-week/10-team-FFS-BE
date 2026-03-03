package com.example.kloset_lab.chat.infrastructure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * 채팅 Redis 캐시 레이어
 *
 * <p>키 구조:
 * <ul>
 *   <li>{@code chat:rooms:{userId}} - Sorted Set (score: lastMessageAt 밀리초, member: roomId)</li>
 *   <li>{@code chat:room:{roomId}:last} - Hash (lastMessageId, lastMessageContent, lastMessageType, lastMessageAt)</li>
 *   <li>{@code chat:unread:{userId}:{roomId}} - String (안읽은 메시지 수)</li>
 * </ul>
 */
@Repository
@RequiredArgsConstructor
public class ChatRedisRepository {

    private static final String ROOMS_KEY = "chat:rooms:%d";
    private static final String LAST_KEY = "chat:room:%d:last";
    private static final String UNREAD_KEY = "chat:unread:%d:%d";

    private final StringRedisTemplate redisTemplate;

    // ======================== Sorted Set (채팅 목록) ========================

    /**
     * 사용자의 채팅방 목록에 방 추가/갱신
     *
     * @param userId    사용자 ID
     * @param roomId    채팅방 ID
     * @param score     정렬 점수 (lastMessageAt 밀리초)
     */
    public void addRoomToUser(Long userId, Long roomId, double score) {
        redisTemplate.opsForZSet().add(roomsKey(userId), String.valueOf(roomId), score);
    }

    /**
     * 사용자의 채팅방 목록에서 방 제거
     *
     * @param userId 사용자 ID
     * @param roomId 채팅방 ID
     */
    public void removeRoomFromUser(Long userId, Long roomId) {
        redisTemplate.opsForZSet().remove(roomsKey(userId), String.valueOf(roomId));
    }

    /**
     * 사용자의 채팅 목록 커서 기반 조회 (최신순)
     *
     * @param userId 사용자 ID
     * @param maxScore 최대 score (커서, 포함)
     * @param size     조회 개수
     * @return roomId 목록 (최신순)
     */
    public List<String> getRoomsDesc(Long userId, double maxScore, int size) {
        Set<String> result = redisTemplate
                .opsForZSet()
                .reverseRangeByScore(roomsKey(userId), Double.NEGATIVE_INFINITY, maxScore, 0, size);
        return result == null ? List.of() : new ArrayList<>(result);
    }

    /**
     * 사용자의 채팅방 목록 전체 카운트 조회
     *
     * @param userId 사용자 ID
     * @return 채팅방 수
     */
    public long getRoomCount(Long userId) {
        Long count = redisTemplate.opsForZSet().zCard(roomsKey(userId));
        return count == null ? 0L : count;
    }

    /**
     * 특정 roomId의 score 조회
     *
     * @param userId 사용자 ID
     * @param roomId 채팅방 ID
     * @return score (없으면 null)
     */
    public Double getRoomScore(Long userId, Long roomId) {
        return redisTemplate.opsForZSet().score(roomsKey(userId), String.valueOf(roomId));
    }

    // ======================== Hash (마지막 메시지 상세) ========================

    /**
     * 채팅방 마지막 메시지 정보 저장
     *
     * @param roomId  채팅방 ID
     * @param fields  Hash 필드 맵
     */
    public void setLastMessage(Long roomId, Map<String, String> fields) {
        redisTemplate.opsForHash().putAll(lastKey(roomId), fields);
    }

    /**
     * 채팅방 마지막 메시지 정보 조회
     *
     * @param roomId 채팅방 ID
     * @return Hash 필드 맵 (없으면 empty)
     */
    public Map<String, String> getLastMessage(Long roomId) {
        Map<Object, Object> raw = redisTemplate.opsForHash().entries(lastKey(roomId));
        Map<String, String> result = new HashMap<>();
        raw.forEach((k, v) -> result.put((String) k, (String) v));
        return result;
    }

    /**
     * 채팅방 마지막 메시지 정보 삭제
     *
     * @param roomId 채팅방 ID
     */
    public void deleteLastMessage(Long roomId) {
        redisTemplate.delete(lastKey(roomId));
    }

    // ======================== Counter (안읽은 메시지 수) ========================

    /**
     * 안읽은 메시지 수 증가
     *
     * @param userId 수신자 사용자 ID
     * @param roomId 채팅방 ID
     * @return 증가 후 값
     */
    public Long incrementUnread(Long userId, Long roomId) {
        return redisTemplate.opsForValue().increment(unreadKey(userId, roomId));
    }

    /**
     * 안읽은 메시지 수 직접 설정 (Redis 재구축 시 MongoDB 기준 복구용)
     *
     * @param userId 사용자 ID
     * @param roomId 채팅방 ID
     * @param count  설정할 안읽은 메시지 수
     */
    public void setUnread(Long userId, Long roomId, long count) {
        redisTemplate.opsForValue().set(unreadKey(userId, roomId), String.valueOf(count));
    }

    /**
     * 안읽은 메시지 수 초기화 (읽음 처리)
     *
     * @param userId 사용자 ID
     * @param roomId 채팅방 ID
     */
    public void resetUnread(Long userId, Long roomId) {
        redisTemplate.opsForValue().set(unreadKey(userId, roomId), "0");
    }

    /**
     * 안읽은 메시지 수 조회
     *
     * @param userId 사용자 ID
     * @param roomId 채팅방 ID
     * @return 안읽은 메시지 수 (없으면 0)
     */
    public long getUnread(Long userId, Long roomId) {
        String value = redisTemplate.opsForValue().get(unreadKey(userId, roomId));
        if (value == null) {
            return 0L;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /**
     * 안읽은 메시지 카운터 키 삭제
     *
     * @param userId 사용자 ID
     * @param roomId 채팅방 ID
     */
    public void deleteUnread(Long userId, Long roomId) {
        redisTemplate.delete(unreadKey(userId, roomId));
    }

    // ======================== 키 생성 헬퍼 ========================

    private String roomsKey(Long userId) {
        return String.format(ROOMS_KEY, userId);
    }

    private String lastKey(Long roomId) {
        return String.format(LAST_KEY, roomId);
    }

    private String unreadKey(Long userId, Long roomId) {
        return String.format(UNREAD_KEY, userId, roomId);
    }
}
