package com.example.kloset_lab.chat.constant;

/** 채팅 도메인 공통 상수 */
public final class ChatConstants {

    // ======================== 메시지 타입 ========================
    public static final String MSG_TYPE_TEXT = "TEXT";
    public static final String MSG_TYPE_IMAGE = "IMAGE";
    public static final String MSG_TYPE_FEED = "FEED";

    // ======================== 컨텐츠 미리보기 ========================
    public static final String PREVIEW_IMAGE = "[이미지]";
    public static final String PREVIEW_FEED = "[피드]";

    // ======================== STOMP 세션 속성 키 ========================
    public static final String SESSION_ATTR_USER_ID = "userId";

    // ======================== Redis Hash 필드명 ========================
    public static final String FIELD_LAST_MESSAGE_ID = "lastMessageId";
    public static final String FIELD_LAST_MESSAGE_CONTENT = "lastMessageContent";
    public static final String FIELD_LAST_MESSAGE_TYPE = "lastMessageType";
    public static final String FIELD_LAST_MESSAGE_AT = "lastMessageAt";

    // ======================== 에러 메시지 ========================
    public static final String ERR_MSG_SEND_FAILED = "메시지 전송에 실패했습니다.";

    // ======================== 기본 페이지 크기 ========================
    public static final int DEFAULT_MESSAGE_PAGE_SIZE = 30;
    public static final int DEFAULT_ROOM_PAGE_SIZE = 20;

    // ======================== 컨텐츠 미리보기 최대 길이 ========================
    public static final int CONTENT_PREVIEW_MAX_LENGTH = 255;

    private ChatConstants() {}
}
