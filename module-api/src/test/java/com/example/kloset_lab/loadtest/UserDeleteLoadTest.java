package com.example.kloset_lab.loadtest;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.kloset_lab.user.service.UserLifecycleService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * 회원 탈퇴 부하 테스트
 *
 * <p>테스트 시나리오:
 * 1. 더미 회원 100명 생성
 * 2. 각 회원이 피드 10개씩 작성 (총 1,000개 피드)
 * 3. 각 회원이 모든 피드에 댓글 1개 + 좋아요
 * 4. 모든 댓글에 좋아요
 * 5. 한 회원 탈퇴 시 다른 99명이 동시에 X Lock 요구하는 작업 수행
 *
 * <p>주의: 이 테스트는 대량의 데이터를 생성하므로 로컬 환경에서만 실행하세요.
 * 예상 데이터량:
 * - user: 100건
 * - feed: 1,000건
 * - feed_like: 100,000건 (100명 x 1,000피드)
 * - comment: 100,000건 (100명 x 1,000피드)
 * - comment_like: 약 100,000건 (단순화된 버전)
 */
@SpringBootTest
@ActiveProfiles("local")
public class UserDeleteLoadTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserLifecycleService userLifecycleService;

    private static final int USER_COUNT = 100;
    private static final int FEEDS_PER_USER = 10;
    private static final int TOTAL_FEEDS = USER_COUNT * FEEDS_PER_USER;
    private static final int BATCH_SIZE = 1000;

    @BeforeEach
    void cleanUp() {
        // 기존 테스트 데이터 정리
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
        jdbcTemplate.execute("TRUNCATE TABLE comment_like");
        jdbcTemplate.execute("TRUNCATE TABLE feed_like");
        jdbcTemplate.execute("TRUNCATE TABLE comment");
        jdbcTemplate.execute("TRUNCATE TABLE feed_image");
        jdbcTemplate.execute("TRUNCATE TABLE feed");
        jdbcTemplate.execute("TRUNCATE TABLE media_file");
        jdbcTemplate.execute("TRUNCATE TABLE user_profile");
        jdbcTemplate.execute("TRUNCATE TABLE refresh_token");
        jdbcTemplate.execute("TRUNCATE TABLE user");
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
    }

    @Test
    @DisplayName("더미 데이터 생성")
    void generateDummyData() {
        System.out.println("=== 더미 데이터 생성 시작 ===");
        long startTime = System.currentTimeMillis();

        // 1. 회원 생성
        generateUsers();

        // 2. 프로필 이미지 생성 (media_file + user_profile 연결)
        generateProfileMediaFiles();

        // 3. 피드 생성
        generateFeeds();

        // 4. 피드 이미지 생성 (media_file + feed_image)
        generateFeedMediaFiles();

        // 5. 피드 좋아요 생성 (모든 회원이 모든 피드에 좋아요)
        generateFeedLikes();

        // 6. 댓글 생성 (모든 회원이 모든 피드에 댓글 1개)
        generateComments();

        // 7. 댓글 좋아요 생성 (단순화: 각 회원이 자신이 작성한 피드의 댓글에 좋아요)
        generateCommentLikes();

        long endTime = System.currentTimeMillis();
        System.out.println("=== 더미 데이터 생성 완료: " + (endTime - startTime) / 1000 + "초 ===");

        // 데이터 검증
        verifyData();
    }

    @Test
    @DisplayName("회원 탈퇴 + 동시 좋아요 요청 부하 테스트")
    void testDeleteUserWithConcurrentLikes() throws Exception {
        // 사전 조건: 더미 데이터가 이미 생성되어 있어야 함
        Long targetUserId = 1L;
        int concurrentUsers = 999;

        ExecutorService executor = Executors.newFixedThreadPool(200);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        AtomicLong totalResponseTime = new AtomicLong(0);

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        // 탈퇴 유저가 좋아요한 피드들의 ID 조회
        List<Long> feedIdsLikedByTarget = jdbcTemplate.queryForList(
                "SELECT feed_id FROM feed_like WHERE user_id = ? LIMIT 100", Long.class, targetUserId);

        System.out.println("=== 부하 테스트 시작 ===");
        System.out.println("탈퇴 대상 유저: " + targetUserId);
        System.out.println("동시 요청 유저 수: " + concurrentUsers);
        System.out.println("대상 피드 수: " + feedIdsLikedByTarget.size());

        long testStartTime = System.currentTimeMillis();

        // 탈퇴 작업 시작 (별도 스레드)
        CompletableFuture<Long> deleteFuture = CompletableFuture.supplyAsync(
                () -> {
                    long start = System.currentTimeMillis();
                    userLifecycleService.deleteUser(targetUserId);
                    return System.currentTimeMillis() - start;
                },
                executor);

        // 동시에 다른 유저들이 같은 피드에 좋아요 시도
        for (int i = 2; i <= concurrentUsers + 1; i++) {
            final long userId = i;
            final Long feedId = feedIdsLikedByTarget.get(i % feedIdsLikedByTarget.size());

            CompletableFuture<Void> future = CompletableFuture.runAsync(
                    () -> {
                        long start = System.currentTimeMillis();
                        try {
                            // 좋아요 토글 (UPDATE feed SET like_count = ... WHERE id = ?)
                            jdbcTemplate.update("UPDATE feed SET like_count = like_count + 1 WHERE id = ?", feedId);
                            successCount.incrementAndGet();
                        } catch (Exception e) {
                            failCount.incrementAndGet();
                            System.out.println("실패 (userId=" + userId + "): " + e.getMessage());
                        }
                        totalResponseTime.addAndGet(System.currentTimeMillis() - start);
                    },
                    executor);

            futures.add(future);
        }

        // 모든 작업 완료 대기
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(5, TimeUnit.MINUTES);

        Long deleteTime = deleteFuture.get();
        long testEndTime = System.currentTimeMillis();

        // 결과 출력
        System.out.println("\n=== 부하 테스트 결과 ===");
        System.out.println("전체 소요 시간: " + (testEndTime - testStartTime) + "ms");
        System.out.println("탈퇴 처리 시간: " + deleteTime + "ms");
        System.out.println("성공한 요청: " + successCount.get());
        System.out.println("실패한 요청: " + failCount.get());
        System.out.println("평균 응답 시간: " + (totalResponseTime.get() / concurrentUsers) + "ms");

        executor.shutdown();

        // 탈퇴 검증
        Integer deletedUserCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user WHERE id = ? AND deleted_at IS NOT NULL", Integer.class, targetUserId);
        assertThat(deletedUserCount).isEqualTo(1);
    }

    private void generateUsers() {
        System.out.println("회원 생성 중... (0/" + USER_COUNT + ")");

        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO user (provider, provider_id, status, role, created_at, updated_at) VALUES ");

        for (int i = 1; i <= USER_COUNT; i++) {
            if ((i - 1) % BATCH_SIZE != 0) sb.append(",");
            sb.append("('KAKAO', 'test_provider_").append(i).append("', 'COMPLETE', 'ROLE_USER', NOW(), NOW())");

            if (i % BATCH_SIZE == 0) {
                jdbcTemplate.execute(sb.toString());
                sb = new StringBuilder();
                sb.append("INSERT INTO user (provider, provider_id, status, role, created_at, updated_at) VALUES ");
                System.out.println("회원 생성 중... (" + i + "/" + USER_COUNT + ")");
            }
        }

        if (USER_COUNT % BATCH_SIZE != 0) {
            String sql = sb.toString();
            if (sql.endsWith("VALUES ")) {
                // 아무것도 추가되지 않음
            } else {
                jdbcTemplate.execute(sql);
            }
        }

        // user_profile 생성
        sb = new StringBuilder();
        sb.append("INSERT INTO user_profile (user_id, nickname, birth_date, gender, created_at) VALUES ");

        for (int i = 1; i <= USER_COUNT; i++) {
            if ((i - 1) % BATCH_SIZE != 0) sb.append(",");
            sb.append("(").append(i).append(", 'testuser").append(i).append("', '1990-01-01', 'MALE', NOW())");

            if (i % BATCH_SIZE == 0) {
                jdbcTemplate.execute(sb.toString());
                sb = new StringBuilder();
                sb.append("INSERT INTO user_profile (user_id, nickname, birth_date, gender, created_at) VALUES ");
            }
        }

        if (USER_COUNT % BATCH_SIZE != 0) {
            String sql = sb.toString();
            if (!sql.endsWith("VALUES ")) {
                jdbcTemplate.execute(sql);
            }
        }

        System.out.println("회원 생성 완료: " + USER_COUNT + "명");
    }

    private void generateProfileMediaFiles() {
        System.out.println("프로필 이미지 생성 중...");

        // 프로필용 media_file 생성 (ID: 1 ~ USER_COUNT)
        StringBuilder sb = new StringBuilder();
        sb.append(
                "INSERT INTO media_file (user_id, purpose, object_key, type, status, uploaded_at, created_at) VALUES ");

        for (int i = 1; i <= USER_COUNT; i++) {
            if (i > 1) sb.append(",");
            sb.append("(")
                    .append(i)
                    .append(", 'PROFILE', 'mock/profile/")
                    .append(i)
                    .append(".jpg', 'JPEG', 'UPLOADED', NOW(), NOW())");
        }
        jdbcTemplate.execute(sb.toString());

        // user_profile.profile_file_id 연결
        jdbcTemplate.update(
                "UPDATE user_profile up INNER JOIN media_file mf ON mf.user_id = up.user_id AND mf.purpose = 'PROFILE' SET up.profile_file_id = mf.id");

        System.out.println("프로필 이미지 생성 완료: " + USER_COUNT + "개");
    }

    private void generateFeedMediaFiles() {
        System.out.println("피드 이미지 생성 중...");

        int mediaFileIdStart = USER_COUNT + 1; // 프로필 이미지가 1 ~ USER_COUNT 사용

        // 피드용 media_file 생성 (피드당 1개)
        int count = 0;
        StringBuilder sb = new StringBuilder();
        sb.append(
                "INSERT INTO media_file (user_id, purpose, object_key, type, status, uploaded_at, created_at) VALUES ");

        for (int feedId = 1; feedId <= TOTAL_FEEDS; feedId++) {
            int userId = ((feedId - 1) / FEEDS_PER_USER) + 1;
            if (count > 0 && count % BATCH_SIZE != 0) sb.append(",");
            sb.append("(")
                    .append(userId)
                    .append(", 'FEED', 'mock/feed/")
                    .append(feedId)
                    .append(".jpg', 'JPEG', 'UPLOADED', NOW(), NOW())");
            count++;

            if (count % BATCH_SIZE == 0) {
                jdbcTemplate.execute(sb.toString());
                sb = new StringBuilder();
                sb.append(
                        "INSERT INTO media_file (user_id, purpose, object_key, type, status, uploaded_at, created_at) VALUES ");
                System.out.println("피드 이미지(media_file) 생성 중... (" + count + "/" + TOTAL_FEEDS + ")");
            }
        }

        if (count % BATCH_SIZE != 0) {
            String sql = sb.toString();
            if (!sql.endsWith("VALUES ")) {
                jdbcTemplate.execute(sql);
            }
        }

        // feed_image 레코드 생성 (피드당 대표 이미지 1개)
        count = 0;
        sb = new StringBuilder();
        sb.append("INSERT INTO feed_image (feed_id, file_id, display_order, is_primary, created_at) VALUES ");

        for (int feedId = 1; feedId <= TOTAL_FEEDS; feedId++) {
            int mediaFileId = mediaFileIdStart + feedId - 1;
            if (count > 0 && count % BATCH_SIZE != 0) sb.append(",");
            sb.append("(").append(feedId).append(", ").append(mediaFileId).append(", 0, 1, NOW())");
            count++;

            if (count % BATCH_SIZE == 0) {
                jdbcTemplate.execute(sb.toString());
                sb = new StringBuilder();
                sb.append("INSERT INTO feed_image (feed_id, file_id, display_order, is_primary, created_at) VALUES ");
                System.out.println("피드 이미지(feed_image) 생성 중... (" + count + "/" + TOTAL_FEEDS + ")");
            }
        }

        if (count % BATCH_SIZE != 0) {
            String sql = sb.toString();
            if (!sql.endsWith("VALUES ")) {
                jdbcTemplate.execute(sql);
            }
        }

        System.out.println("피드 이미지 생성 완료: " + TOTAL_FEEDS + "개");
    }

    private void generateFeeds() {
        System.out.println("피드 생성 중... (0/" + TOTAL_FEEDS + ")");

        int count = 0;
        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO feed (user_id, content, like_count, comment_count, created_at, updated_at) VALUES ");

        for (int userId = 1; userId <= USER_COUNT; userId++) {
            for (int feedNum = 1; feedNum <= FEEDS_PER_USER; feedNum++) {
                if (count % BATCH_SIZE != 0) sb.append(",");
                sb.append("(")
                        .append(userId)
                        .append(", 'Test feed content from user ")
                        .append(userId)
                        .append(" #")
                        .append(feedNum)
                        .append("', 0, 0, NOW(), NOW())");
                count++;

                if (count % BATCH_SIZE == 0) {
                    jdbcTemplate.execute(sb.toString());
                    sb = new StringBuilder();
                    sb.append(
                            "INSERT INTO feed (user_id, content, like_count, comment_count, created_at, updated_at) VALUES ");
                    System.out.println("피드 생성 중... (" + count + "/" + TOTAL_FEEDS + ")");
                }
            }
        }

        if (count % BATCH_SIZE != 0) {
            String sql = sb.toString();
            if (!sql.endsWith("VALUES ")) {
                jdbcTemplate.execute(sql);
            }
        }

        System.out.println("피드 생성 완료: " + TOTAL_FEEDS + "개");
    }

    private void generateFeedLikes() {
        long totalLikes = (long) USER_COUNT * TOTAL_FEEDS;
        System.out.println("피드 좋아요 생성 중... (0/" + totalLikes + ")");
        System.out.println("주의: 대량 데이터 생성으로 시간이 오래 걸릴 수 있습니다.");

        long count = 0;
        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO feed_like (feed_id, user_id, created_at) VALUES ");

        for (int feedId = 1; feedId <= TOTAL_FEEDS; feedId++) {
            for (int userId = 1; userId <= USER_COUNT; userId++) {
                if (count > 0 && count % BATCH_SIZE != 0) sb.append(",");
                sb.append("(").append(feedId).append(",").append(userId).append(", NOW())");
                count++;

                if (count % BATCH_SIZE == 0) {
                    jdbcTemplate.execute(sb.toString());
                    sb = new StringBuilder();
                    sb.append("INSERT INTO feed_like (feed_id, user_id, created_at) VALUES ");

                    // like_count 업데이트 (배치 단위로)
                    if (count % (BATCH_SIZE * 10) == 0) {
                        System.out.println("피드 좋아요 생성 중... (" + count + "/" + totalLikes + ")");
                    }
                }
            }

            // 각 피드의 like_count 업데이트
            jdbcTemplate.update("UPDATE feed SET like_count = ? WHERE id = ?", USER_COUNT, feedId);
        }

        if (count % BATCH_SIZE != 0) {
            String sql = sb.toString();
            if (!sql.endsWith("VALUES ")) {
                jdbcTemplate.execute(sql);
            }
        }

        System.out.println("피드 좋아요 생성 완료: " + count + "개");
    }

    private void generateComments() {
        long totalComments = (long) USER_COUNT * TOTAL_FEEDS;
        System.out.println("댓글 생성 중... (0/" + totalComments + ")");

        long count = 0;
        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO comment (feed_id, user_id, content, like_count, created_at, updated_at) VALUES ");

        for (int feedId = 1; feedId <= TOTAL_FEEDS; feedId++) {
            for (int userId = 1; userId <= USER_COUNT; userId++) {
                if (count % BATCH_SIZE != 0) sb.append(",");
                sb.append("(")
                        .append(feedId)
                        .append(",")
                        .append(userId)
                        .append(", 'Comment by user ")
                        .append(userId)
                        .append("', 0, NOW(), NOW())");
                count++;

                if (count % BATCH_SIZE == 0) {
                    jdbcTemplate.execute(sb.toString());
                    sb = new StringBuilder();
                    sb.append(
                            "INSERT INTO comment (feed_id, user_id, content, like_count, created_at, updated_at) VALUES ");

                    if (count % (BATCH_SIZE * 10) == 0) {
                        System.out.println("댓글 생성 중... (" + count + "/" + totalComments + ")");
                    }
                }
            }

            // 각 피드의 comment_count 업데이트
            jdbcTemplate.update("UPDATE feed SET comment_count = ? WHERE id = ?", USER_COUNT, feedId);
        }

        if (count % BATCH_SIZE != 0) {
            String sql = sb.toString();
            if (!sql.endsWith("VALUES ")) {
                jdbcTemplate.execute(sql);
            }
        }

        System.out.println("댓글 생성 완료: " + count + "개");
    }

    private void generateCommentLikes() {
        // 단순화: 각 유저가 자신이 작성한 피드의 모든 댓글에 좋아요
        // 이렇게 하면 약 USER_COUNT * USER_COUNT * FEEDS_PER_USER 개의 좋아요가 생성됨
        System.out.println("댓글 좋아요 생성 중... (단순화된 버전)");

        long count = 0;

        // 각 피드 작성자가 자신의 피드에 달린 모든 댓글에 좋아요
        for (int feedId = 1; feedId <= TOTAL_FEEDS; feedId++) {
            int feedOwnerId = ((feedId - 1) / FEEDS_PER_USER) + 1;

            StringBuilder sb = new StringBuilder();
            sb.append("INSERT INTO comment_like (comment_id, user_id, created_at) ");
            sb.append("SELECT c.id, ").append(feedOwnerId).append(", NOW() ");
            sb.append("FROM comment c WHERE c.feed_id = ").append(feedId);

            jdbcTemplate.execute(sb.toString());

            // comment의 like_count 업데이트
            jdbcTemplate.update("UPDATE comment SET like_count = 1 WHERE feed_id = ? AND like_count = 0", feedId);

            count += USER_COUNT;

            if (feedId % 1000 == 0) {
                System.out.println("댓글 좋아요 생성 중... (피드 " + feedId + "/" + TOTAL_FEEDS + ")");
            }
        }

        System.out.println("댓글 좋아요 생성 완료: 약 " + count + "개");
    }

    private void verifyData() {
        System.out.println("\n=== 데이터 검증 ===");

        Integer userCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user", Integer.class);
        Integer feedCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM feed", Integer.class);
        Long mediaFileCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM media_file", Long.class);
        Long feedImageCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM feed_image", Long.class);
        Long feedLikeCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM feed_like", Long.class);
        Long commentCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM comment", Long.class);
        Long commentLikeCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM comment_like", Long.class);
        Integer profileWithImageCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_profile WHERE profile_file_id IS NOT NULL", Integer.class);

        System.out.println("user: " + userCount);
        System.out.println("feed: " + feedCount);
        System.out.println("media_file: " + mediaFileCount + " (프로필 " + USER_COUNT + " + 피드 " + TOTAL_FEEDS + ")");
        System.out.println("feed_image: " + feedImageCount);
        System.out.println("user_profile (이미지 연결됨): " + profileWithImageCount);
        System.out.println("feed_like: " + feedLikeCount);
        System.out.println("comment: " + commentCount);
        System.out.println("comment_like: " + commentLikeCount);

        // 첫 번째 피드의 like_count, comment_count 확인
        Integer firstFeedLikeCount =
                jdbcTemplate.queryForObject("SELECT like_count FROM feed WHERE id = 1", Integer.class);
        Integer firstFeedCommentCount =
                jdbcTemplate.queryForObject("SELECT comment_count FROM feed WHERE id = 1", Integer.class);

        System.out.println("\n첫 번째 피드:");
        System.out.println("  like_count: " + firstFeedLikeCount);
        System.out.println("  comment_count: " + firstFeedCommentCount);

        assertThat(userCount).isEqualTo(USER_COUNT);
        assertThat(feedCount).isEqualTo(TOTAL_FEEDS);
        assertThat(mediaFileCount).isEqualTo((long) USER_COUNT + TOTAL_FEEDS);
        assertThat(feedImageCount).isEqualTo((long) TOTAL_FEEDS);
        assertThat(profileWithImageCount).isEqualTo(USER_COUNT);
    }
}
