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

    /**
     * 메시지 타입·내용으로 미리보기 텍스트 생성
     *
     * @param type    메시지 타입 (TEXT / IMAGE / FEED)
     * @param content 원본 텍스트 (IMAGE·FEED 타입이면 무시)
     * @return 미리보기 텍스트
     */
    public static String toPreview(String type, String content) {
        return switch (type) {
            case MSG_TYPE_IMAGE -> PREVIEW_IMAGE;
            case MSG_TYPE_FEED -> PREVIEW_FEED;
            default -> content != null
                    ? (content.length() > CONTENT_PREVIEW_MAX_LENGTH
                            ? content.substring(0, CONTENT_PREVIEW_MAX_LENGTH)
                            : content)
                    : "";
        };
    }

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
