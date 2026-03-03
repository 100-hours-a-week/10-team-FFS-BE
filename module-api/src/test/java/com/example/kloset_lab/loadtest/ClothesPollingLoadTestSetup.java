package com.example.kloset_lab.loadtest;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.kloset_lab.global.security.TokenType;
import com.example.kloset_lab.global.security.provider.JwtTokenProvider;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * 옷 분석 폴링 부하테스트 데이터 셋업
 *
 * <p>테스트 시나리오: 수많은 사용자가 옷 분석 요청 후 결과를 기다리며 폴링하는 상황을 시뮬레이션한다.
 * GET /api/v1/clothes/analyses/{batchId} 엔드포인트에 대한 동시 부하를 측정한다.
 *
 * <p>실행 순서:
 *
 * <ol>
 *   <li>Docker DB 실행: docker-compose up -d
 *   <li>앱 실행: ./gradlew bootRun
 *   <li>데이터 생성: ./gradlew test --tests "*.loadtest.ClothesPollingLoadTestSetup.setupData"
 *   <li>콘솔에 출력된 k6 명령어 실행
 *   <li>테스트 완료 후 정리: ./gradlew test --tests "*.loadtest.ClothesPollingLoadTestSetup.cleanupData"
 * </ol>
 *
 * <p>테스트 모드 (IS_FINISHED 설정):
 *
 * <ul>
 *   <li>true (기본): 완료된 배치를 폴링 → DB 조회만 발생 (순수 백엔드 부하 측정)
 *   <li>false: 미완료 배치를 폴링 → AI 서버 호출 포함 (AI 서버 실행 필요)
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("local")
public class ClothesPollingLoadTestSetup {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    /** 생성할 배치 수 (동시 폴링 대상 수) */
    private static final int BATCH_COUNT = 200;

    /** 배치당 태스크 수 (일반적으로 사용자가 동시에 분석하는 옷 개수) */
    private static final int TASKS_PER_BATCH = 3;

    /**
     * 배치 완료 여부 설정
     *
     * <ul>
     *   <li>true: 이미 완료된 배치 → 매 폴링 시 DB 조회만 발생 (AI 서버 호출 없음)
     *   <li>false: 미완료 배치 → 매 폴링 시 AI 서버 호출 발생 (AI 서버 실행 필요)
     * </ul>
     */
    private static final boolean IS_FINISHED = true;

    private static final Long TEST_USER_ID = 1L;
    private static final String BATCH_ID_PREFIX = "lt_batch_";
    private static final String TASK_ID_PREFIX = "lt_task_";

    @Test
    @DisplayName("폴링 부하테스트 더미 데이터 생성")
    void setupData() {
        System.out.println("=== 폴링 부하테스트 데이터 셋업 시작 ===");
        long startTime = System.currentTimeMillis();

        ensureTestUserExists();
        cleanTestData();
        createBatches();
        createTasks();
        verifyData();

        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println("=== 데이터 셋업 완료: " + elapsed + "ms ===\n");

        printK6Command();
    }

    @Test
    @DisplayName("폴링 부하테스트 데이터 정리")
    void cleanupData() {
        cleanTestData();
        System.out.println("테스트 데이터 정리 완료");
    }

    private void ensureTestUserExists() {
        Integer count =
                jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user WHERE id = ?", Integer.class, TEST_USER_ID);

        if (count == null || count == 0) {
            jdbcTemplate.update(
                    "INSERT INTO user (id, provider, provider_id, status, role, created_at, updated_at) "
                            + "VALUES (?, 'KAKAO', 'loadtest_provider_1', 'COMPLETE', 'ROLE_USER', NOW(), NOW())",
                    TEST_USER_ID);

            jdbcTemplate.update(
                    "INSERT INTO user_profile (user_id, nickname, birth_date, gender, created_at) "
                            + "VALUES (?, 'loadtest_user', '1990-01-01', 'MALE', NOW())",
                    TEST_USER_ID);

            System.out.println("테스트 유저 생성 (ID: " + TEST_USER_ID + ")");
        } else {
            System.out.println("테스트 유저 존재 확인 (ID: " + TEST_USER_ID + ")");
        }
    }

    private void cleanTestData() {
        int taskDeleted =
                jdbcTemplate.update("DELETE FROM temp_clothes_task WHERE task_id LIKE ?", TASK_ID_PREFIX + "%");
        int batchDeleted =
                jdbcTemplate.update("DELETE FROM temp_clothes_batch WHERE batch_id LIKE ?", BATCH_ID_PREFIX + "%");
        System.out.println("기존 테스트 데이터 삭제 (배치: " + batchDeleted + ", 태스크: " + taskDeleted + ")");
    }

    private void createBatches() {
        String status = IS_FINISHED ? "COMPLETED" : "IN_PROGRESS";
        int completed = IS_FINISHED ? TASKS_PER_BATCH : 0;
        int processing = IS_FINISHED ? 0 : TASKS_PER_BATCH;
        int isFinishedValue = IS_FINISHED ? 1 : 0;

        StringBuilder sb = new StringBuilder();
        sb.append(
                "INSERT INTO temp_clothes_batch (user_id, batch_id, status, total, completed, processing, is_finished, created_at, updated_at) VALUES ");

        for (int i = 1; i <= BATCH_COUNT; i++) {
            if (i > 1) sb.append(",");
            String batchId = String.format("%s%04d", BATCH_ID_PREFIX, i);
            sb.append("(")
                    .append(TEST_USER_ID)
                    .append(", '")
                    .append(batchId)
                    .append("', '")
                    .append(status)
                    .append("', ")
                    .append(TASKS_PER_BATCH)
                    .append(", ")
                    .append(completed)
                    .append(", ")
                    .append(processing)
                    .append(", ")
                    .append(isFinishedValue)
                    .append(", NOW(), NOW())");
        }

        jdbcTemplate.execute(sb.toString());
        System.out.println("배치 생성 완료: " + BATCH_COUNT + "개 (모드: " + (IS_FINISHED ? "완료" : "미완료") + ")");
    }

    private void createTasks() {
        String taskStatus = IS_FINISHED ? "COMPLETED" : "PREPROCESSING";

        // 배치 PK 조회 (temp_clothes_task.batch_id FK는 auto-increment PK를 참조)
        List<Map<String, Object>> batches = jdbcTemplate.queryForList(
                "SELECT id, batch_id FROM temp_clothes_batch WHERE batch_id LIKE ? ORDER BY id", BATCH_ID_PREFIX + "%");

        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO temp_clothes_task (batch_id, task_id, status, created_at, updated_at) VALUES ");

        boolean first = true;
        for (Map<String, Object> batch : batches) {
            Long batchPk = ((Number) batch.get("id")).longValue();
            String batchIdStr = (String) batch.get("batch_id");
            String batchNum = batchIdStr.replace(BATCH_ID_PREFIX, "");

            for (int taskNum = 1; taskNum <= TASKS_PER_BATCH; taskNum++) {
                if (!first) sb.append(",");
                first = false;

                String taskId = String.format("%s%s_%d", TASK_ID_PREFIX, batchNum, taskNum);
                sb.append("(")
                        .append(batchPk)
                        .append(", '")
                        .append(taskId)
                        .append("', '")
                        .append(taskStatus)
                        .append("', NOW(), NOW())");
            }
        }

        jdbcTemplate.execute(sb.toString());
        System.out.println("태스크 생성 완료: " + (batches.size() * TASKS_PER_BATCH) + "개");
    }

    private void verifyData() {
        Integer batchCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM temp_clothes_batch WHERE batch_id LIKE ?", Integer.class, BATCH_ID_PREFIX + "%");
        Integer taskCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM temp_clothes_task WHERE task_id LIKE ?", Integer.class, TASK_ID_PREFIX + "%");

        System.out.println("\n=== 데이터 검증 ===");
        System.out.println("  배치: " + batchCount + "개 (예상: " + BATCH_COUNT + ")");
        System.out.println("  태스크: " + taskCount + "개 (예상: " + (BATCH_COUNT * TASKS_PER_BATCH) + ")");

        assertThat(batchCount).isEqualTo(BATCH_COUNT);
        assertThat(taskCount).isEqualTo(BATCH_COUNT * TASKS_PER_BATCH);
    }

    private void printK6Command() {
        String token = jwtTokenProvider.generateAccessToken(TEST_USER_ID, TokenType.ACTIVE);

        System.out.println("========================================");
        System.out.println("k6 실행 명령어:");
        System.out.println();
        System.out.println("k6 run \\");
        System.out.println("  -e TOKEN=" + token + " \\");
        System.out.println("  -e BATCH_COUNT=" + BATCH_COUNT + " \\");
        System.out.println("  k6/scenario-clothes-polling.js");
        System.out.println();
        System.out.println("옵션:");
        System.out.println("  -e BASE_URL=<URL>       서버 주소 (기본: http://localhost:8080)");
        System.out.println("  -e POLL_INTERVAL=<초>   폴링 간격 (기본: 2)");
        System.out.println("========================================");
    }
}
