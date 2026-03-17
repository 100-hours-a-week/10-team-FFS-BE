package com.example.kloset_lab.global.response;

public class Message {
    // Auth
    public static final String LOGIN_SUCCEEDED = "login_succeeded";
    public static final String REGISTRATION_REQUIRED = "registration_required";
    public static final String ACCESS_TOKEN_REFRESHED = "access_token_refreshed";
    public static final String LOGOUT_SUCCEEDED = "logout_succeeded";

    // File Upload
    public static final String PRESIGNED_URL_GENERATED = "presigned_url_generated";

    // User
    public static final String USER_CREATED = "user_created";
    public static final String USER_DELETED = "user_deleted";
    public static final String NICKNAME_CHECKED_UNIQUE = "nickname_checked_unique";
    public static final String NICKNAME_CHECKED_DUPLICATE = "nickname_checked_duplicate";
    public static final String BIRTH_DATE_VALID = "birth_date_valid";
    public static final String BIRTH_DATE_INVALID_FORMAT = "birth_date_invalid_format";
    public static final String BIRTH_DATE_FUTURE = "birth_date_future";
    public static final String BIRTH_DATE_TOO_OLD = "birth_date_too_old";
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
    public static final String CLOTHES_FEED_DETAIL = "clothes_feed_detail";
    public static final String CLOTHES_FEED = "clothes_feed_retrieved";

    // Feed
    public static final String FEED_CREATED = "feed_created";
    public static final String FEED_EDITED = "feed_edited";
    public static final String FEED_DELETED = "feed_deleted";
    public static final String FEED_RETRIEVED = "feed_retrieved";
    public static final String FEEDS_RETRIEVED = "feeds_retrieved";
    public static final String FEED_LIKES_RETRIEVED = "feed_likes_list_retrieved";
    public static final String FEED_LIKED = "like_feed";
    public static final String FEED_LIKE_CANCELLED = "cancle_like_feed";
    public static final String FOLLOWING_FEED_RETRIEVED = "following_feeds_fetched";

    // Comment
    public static final String COMMENTS_RETRIEVED = "comments_retrieved";
    public static final String COMMENT_CREATED = "comment_created";
    public static final String COMMENT_UPDATED = "comment_updated";
    public static final String COMMENT_DELETED = "comment_deleted";
    public static final String COMMENT_LIKED = "like_comment";
    public static final String COMMENT_LIKE_CANCELLED = "cancle_like_comment";

    // Chat
    public static final String CHAT_ROOM_CREATED = "chat_room_created";
    public static final String CHAT_ROOM_RETRIEVED = "chat_room_retrieved";
    public static final String CHAT_ROOMS_RETRIEVED = "chat_rooms_retrieved";
    public static final String CHAT_MESSAGES_RETRIEVED = "chat_messages_retrieved";
    public static final String CHAT_ROOM_LEFT = "chat_room_left";
    public static final String CHAT_MARKED_AS_READ = "chat_marked_as_read";
    public static final String CHAT_UNREAD_STATUS_RETRIEVED = "chat_unread_status_retrieved";
    public static final String CHAT_UNREAD_MESSAGES_RETRIEVED = "chat_unread_messages_retrieved";

    // TPO
    public static final String RECENT_TPO_REQUESTS_RETRIEVED = "recent_tpo_requests_retrieved";
    public static final String TPO_OUTFITS_RETRIEVED = "tpo_outfits_retrieved";
    public static final String REACTION_RECORDED = "reaction_recorded";
    public static final String PRODUCTS_RETRIEVED = "products_retrieved";
    public static final String OUTFIT_REQUEST_ACCEPTED = "outfit_request_accepted";
    public static final String SESSION_LIST_RETRIEVED = "session_list_retrieved";
    public static final String SESSION_DETAIL_RETRIEVED = "session_detail_retrieved";

    // Shopping
    public static final String PRODUCTS_FETCHED = "products_fetched";

    // Follow
    public static final String FOLLOW_CREATED = "following_created";
    public static final String FOLLOW_DELETED = "following_removed";
    public static final String FOLLOWING_RETRIEVED = "following_fetched";
    public static final String FOLLOWER_RETRIEVED = "followers_fetched";
}
