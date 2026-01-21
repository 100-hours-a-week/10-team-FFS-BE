package com.example.kloset_lab.global.response;

public class Message {
    // Auth
    public static final String REGISTER_SUCCEEDED = "register_succeeded";
    public static final String TEMPORARY_REGISTER_SUCCEEDED = "temporary_register_succeeded";
    public static final String ACCESS_TOKEN_REFRESHED = "access_token_refreshed";
    public static final String LOGOUT_SUCCEEDED = "logout_succeeded";

    // File Upload
    public static final String PRESIGNED_URL_GENERATED = "presigned_url_generated";

    // User
    public static final String USER_CREATED = "user_created";
    public static final String NICKNAME_CHECKED_UNIQUE = "nickname_checked_unique";
    public static final String NICKNAME_CHECKED_DUPLICATE = "nickname_checked_duplicate";
    public static final String PROFILE_IMAGE_UPDATED = "profile_image_updated";
    public static final String PROFILE_IMAGE_DELETED = "profile_image_deleted";
    public static final String NICKNAME_UPDATED = "nickname_updated";
    public static final String PROFILE_RETRIEVED = "profile_retrieved";
    public static final String USER_FEEDS_RETRIEVED = "user_feeds_retrieved";
    public static final String USERS_SEARCHED = "users_searched";
    public static final String CLOTHES_COUNT_RETRIEVED = "clothes_count_retrieved";
    public static final String CLOTHES_LIST_RETRIEVED = "clothes_list_retrieved";

    // Clothes
    public static final String AI_PRECHECK_COMPLETED = "ai_precheck_completed";
    public static final String CLOTHES_POLLING_RESULT_RETRIEVED = "clothes_polling_result_retrieved";
    public static final String CLOTHES_CREATED = "clothes_created";
    public static final String CLOTHES_DETAIL_RETRIEVED = "clothes_detail_retrieved";
    public static final String CLOTHES_DETAIL_UPDATED = "clothes_detail_updated";
    public static final String CLOTHES_DELETED = "clothes_deleted";

    // Feed
    public static final String FEED_CREATED = "feed_created";
    public static final String FEED_UPDATED = "feed_updated";
    public static final String FEED_DELETED = "feed_deleted";
    public static final String FEED_RETRIEVED = "feed_retrieved";
    public static final String FEEDS_RETRIEVED = "feeds_retrieved";
    public static final String FEED_LIKES_RETRIEVED = "feed_likes_retrieved";
    public static final String FEED_LIKED = "feed_liked";
    public static final String FEED_LIKE_CANCELLED = "feed_like_cancelled";

    // Comment
    public static final String COMMENTS_RETRIEVED = "comments_retrieved";
    public static final String COMMENT_CREATED = "comment_created";
    public static final String COMMENT_UPDATED = "comment_updated";
    public static final String COMMENT_DELETED = "comment_deleted";
    public static final String COMMENT_LIKE_CREATED = "comment_like_created";
    public static final String COMMENT_LIKE_DELETED = "comment_like_deleted";

    // TPO
    public static final String RECENT_TPO_REQUESTS_RETRIEVED = "recent_tpo_requests_retrieved";
    public static final String TPO_OUTFITS_RETRIEVED = "tpo_outfits_retrieved";
    public static final String REACTION_RECORDED = "reaction_recorded";
    public static final String PRODUCTS_RETRIEVED = "products_retrieved";
}
