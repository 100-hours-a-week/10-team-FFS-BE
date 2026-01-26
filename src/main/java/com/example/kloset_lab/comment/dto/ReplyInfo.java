package com.example.kloset_lab.comment.dto;

import lombok.Builder;

@Builder
public record ReplyInfo (
        long replyCount, boolean hasReplies
){
}
