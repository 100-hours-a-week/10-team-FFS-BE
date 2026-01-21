package com.example.kloset_lab.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    // 400 Bad Request
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "invalid_request"),
    INVALID_NICKNAME(HttpStatus.BAD_REQUEST, "invalid_nickname"),
    MAXIMUM_10_FILES_ALLOWED(HttpStatus.BAD_REQUEST, "maximum_10_files_allowed"),
    MINIMUM_1_FILE_ALLOWED(HttpStatus.BAD_REQUEST, "minimum_1_file_allowed"),
    MAXIMUM_5_FILES_ALLOWED(HttpStatus.BAD_REQUEST, "maximum_5_files_allowed"),
    MAXIMUM_10_CLOTHES_MAPPING_ALLOWED(HttpStatus.BAD_REQUEST, "maximum_10_clothes_mapping_allowed"),
    CONTENT_TOO_SHORT(HttpStatus.BAD_REQUEST, "content_too_short"),
    TOO_MANY_FILES(HttpStatus.BAD_REQUEST, "too_many_files"),
    TOO_FEW_FILES(HttpStatus.BAD_REQUEST, "too_few_files"),
    UNSUPPORTED_FILE_TYPE(HttpStatus.BAD_REQUEST, "unsupported_file_type"),
    FILE_SIZE_EXCEEDS_10MB(HttpStatus.BAD_REQUEST, "file_size_exceeds_10mb"),

    // 401 Unauthorized
    ACCESS_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "access_token_expired"),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "refresh_token_expired"),

    // 403 Forbidden
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "access_denied"),
    FILE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "file_access_denied"),
    CLOTHES_EDIT_DENIED(HttpStatus.FORBIDDEN, "clothes_edit_denied"),
    CLOTHES_DELETE_DENIED(HttpStatus.FORBIDDEN, "clothes_delete_denied"),
    CLOTHES_ACCESS_DENIED(HttpStatus.FORBIDDEN, "clothes_access_denied"),
    FEED_DELETE_DENIED(HttpStatus.FORBIDDEN, "feed_delete_denied"),
    COMMENT_EDIT_DENIED(HttpStatus.FORBIDDEN, "comment_edit_denied"),
    COMMENT_DELETE_DENIED(HttpStatus.FORBIDDEN, "comment_delete_denied"),
    TPO_RESULT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "tpo_result_access_denied"),
    SHOP_RESULT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "shop_result_access_denied"),

    // 404 Not Found
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "user_not_found"),
    TARGET_USER_NOT_FOUND(HttpStatus.NOT_FOUND, "target_user_not_found"),
    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "file_not_found"),
    TASK_NOT_FOUND(HttpStatus.NOT_FOUND, "task_not_found"),
    CLOTHES_NOT_FOUND(HttpStatus.NOT_FOUND, "clothes_not_found"),
    CLOTHES_DETAIL_NOT_FOUND(HttpStatus.NOT_FOUND, "clothes_detail_not_found"),
    FEED_NOT_FOUND(HttpStatus.NOT_FOUND, "feed_not_found"),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "comment_not_found"),
    PARENT_COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "parent_comment_not_found"),
    OUTFIT_NOT_FOUND(HttpStatus.NOT_FOUND, "outfit_not_found"),

    // 409 Conflict
    EXISTING_NICKNAME(HttpStatus.CONFLICT, "existing_nickname"),
    ALREADY_EXIST_NICKNAME(HttpStatus.CONFLICT, "already_existing_nickname"),
    NOT_PENDING_STATE(HttpStatus.CONFLICT, "not_pending_state"),
    UPLOADED_FILE_MISMATCH(HttpStatus.CONFLICT, "uploaded_file_mismatch"),

    // 413 Payload Too Large
    CONTENT_TOO_LONG(HttpStatus.PAYLOAD_TOO_LARGE, "content_too_long"),
    FILE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "file_too_large"),

    // 422 Unprocessable Entity
    EMPTY_CLOSET(HttpStatus.UNPROCESSABLE_ENTITY, "empty_closet"),
    INSUFFICIENT_ITEMS(HttpStatus.UNPROCESSABLE_ENTITY, "insufficient_items"),
    NO_MATCHED_ITEMS(HttpStatus.UNPROCESSABLE_ENTITY, "no_matched_items"),

    // 429 Too Many Requests
    RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "rate_limit_exceeded"),

    // 500 Internal Server Error
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "internal_server_error"),
    IMAGE_PROCESSING_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "image_processing_error"),

    // 502 Bad Gateway
    AI_ERROR(HttpStatus.BAD_GATEWAY, "ai_error"),

    // 503 Service Unavailable
    AI_TIMEOUT(HttpStatus.SERVICE_UNAVAILABLE, "ai_timeout"),
    AI_SERVER_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "ai_server_error"),
    PROVIDER_SERVER_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "provider_server_error");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
