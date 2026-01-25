package com.example.kloset_lab.global.constants;

/**
 * 페이지네이션 기본값 상수
 * <p>
 * @RequestParam의 defaultValue는 String 타입만 허용하므로 String 상수로 정의
 */
public final class PaginationDefaults {

    /** 피드 목록 조회 기본 개수 */
    public static final String FEED_LIST = "10";

    /** 좋아요 사용자 목록 조회 기본 개수 */
    public static final String LIKE_USER_LIST = "20";

    /** 댓글 목록 조회 기본 개수 */
    public static final String COMMENT_LIST = "20";

    /** 대댓글 목록 조회 기본 개수 */
    public static final String REPLY_LIST = "20";

    private PaginationDefaults() {}
}
