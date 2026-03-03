package com.example.kloset_lab.comment.dto;

import com.example.kloset_lab.user.dto.UserProfileDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import lombok.Builder;

/**
 * 댓글 목록 아이템 DTO
 *
 * @param commentId 댓글 ID
 * @param content 댓글 내용
 * @param likeCount 좋아요 수
 * @param createdAt 작성일시
 * @param userProfile 작성자 프로필
 * @param isLiked 현재 사용자의 좋아요 여부
 * @param isOwner 현재 사용자가 작성자인지 여부
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CommentItem(
        Long commentId,
        String content,
        long likeCount,
        UserProfileDto userProfile,
        LocalDateTime modifiedAt,
        boolean isLiked,
        boolean isOwner,
        ReplyInfo replyInfo) {}
