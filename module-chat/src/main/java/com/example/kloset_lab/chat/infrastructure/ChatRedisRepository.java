package com.example.kloset_lab.chat.infrastructure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
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
     * 사용자의 채팅방 목록 커서 기반 조회 — roomId와 score를 함께 반환 (최신순)
     *
     * <p>ZREVRANGEBYSCORE WITHSCORES 단일 커맨드로 roomId + lastMessageAt score를 동시에 반환하여
     * 이후 ZSCORE N회 재조회를 제거한다.
     *
     * @param userId   사용자 ID
     * @param maxScore 최대 score (커서, 포함)
     * @param size     조회 개수
     * @return roomId → score 맵 (삽입 순서 = 최신순 유지)
     */
    public Map<String, Double> getRoomsDescWithScores(Long userId, double maxScore, int size) {
        Set<TypedTuple<String>> result = redisTemplate
                .opsForZSet()
                .reverseRangeByScoreWithScores(roomsKey(userId), Double.NEGATIVE_INFINITY, maxScore, 0, size);
        if (result == null || result.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Double> map = new LinkedHashMap<>();
        for (TypedTuple<String> tuple : result) {
            if (tuple.getValue() != null && tuple.getScore() != null) {
                map.put(tuple.getValue(), tuple.getScore());
            }
        }
        return map;
    }

    /**
     * 복수 채팅방의 마지막 메시지 정보를 Pipeline으로 일괄 조회
     *
     * <p>HGETALL N회 개별 왕복 → 단일 Pipeline 왕복으로 대체
     *
     * @param roomIds 채팅방 ID 목록 (순서 보존)
     * @return roomId → 마지막 메시지 필드 맵 (순서 보존)
     */
    @SuppressWarnings("unchecked")
    public Map<Long, Map<String, String>> getLastMessagesBatch(List<Long> roomIds) {
        if (roomIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Object> raw = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (Long roomId : roomIds) {
                connection.hashCommands().hGetAll(lastKey(roomId).getBytes());
            }
            return null;
        });

        Map<Long, Map<String, String>> result = new LinkedHashMap<>();
        for (int i = 0; i < roomIds.size(); i++) {
            Map<String, String> fields = (Map<String, String>) raw.get(i);
            result.put(roomIds.get(i), fields != null ? fields : Collections.emptyMap());
        }
        return result;
    }

    /**
     * 복수 채팅방의 안읽은 메시지 수를 MGET으로 일괄 조회
     *
     * <p>GET N회 개별 왕복 → MGET 단일 커맨드로 대체
     *
     * @param userId  사용자 ID
     * @param roomIds 채팅방 ID 목록 (순서 보존)
     * @return roomId → 안읽은 메시지 수 맵
     */
    public Map<Long, Long> getUnreadBatch(Long userId, List<Long> roomIds) {
        if (roomIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<String> keys =
                roomIds.stream().map(roomId -> unreadKey(userId, roomId)).toList();
        List<String> values = redisTemplate.opsForValue().multiGet(keys);

        Map<Long, Long> result = new LinkedHashMap<>();
        for (int i = 0; i < roomIds.size(); i++) {
            String value = (values != null) ? values.get(i) : null;
            long count = 0L;
            if (value != null) {
                try {
                    count = Long.parseLong(value);
                } catch (NumberFormatException ignored) {
                }
            }
            result.put(roomIds.get(i), count);
        }
        return result;
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
     */
    public void incrementUnread(Long userId, Long roomId) {
        redisTemplate.opsForValue().increment(unreadKey(userId, roomId));
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
