package com.example.kloset_lab.chat.document;

import lombok.Builder;
import lombok.Getter;

/** 채팅 메시지 내 이미지 정보 (임베디드 도큐먼트) */
@Getter
@Builder
public class ChatImage {

    /** MySQL media_file ID */
    private Long mediaFileId;

    /** S3 object key */
    private String objectKey;

    /** 표시 순서 (0부터 시작) */
    private int displayOrder;
}
