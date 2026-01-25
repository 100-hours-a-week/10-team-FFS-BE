package com.example.kloset_lab.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 댓글 작성 요청 DTO
 *
 * @param content 댓글 내용
 * @param parentId 부모 댓글 ID (대댓글일 경우에만 포함, 원댓글이면 null)
 */
public record CommentCreateRequest(@NotBlank @Size(max = 500) String content, Long parentId) {}
