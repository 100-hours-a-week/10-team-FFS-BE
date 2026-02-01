package com.example.kloset_lab.comment.dto;

import com.example.kloset_lab.global.exception.ErrorCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 댓글 수정 요청 DTO
 *
 * @param content 수정할 댓글 내용
 */
public record CommentUpdateRequest(@NotBlank @Size(max = 500, message = ErrorCode.Code.CONTENT_TOO_LONG) String content) {}
