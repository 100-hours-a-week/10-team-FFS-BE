package com.example.kloset_lab.chat.document;

import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/** 채팅 메시지 MongoDB 도큐먼트 */
@Document(collection = "chat_messages")
@CompoundIndex(def = "{'roomId': 1, '_id': -1}")
@Getter
@Builder
public class ChatMessage {

    @Id
    private ObjectId id;

    @Field("roomId")
    private Long roomId;

    @Field("senderId")
    private Long senderId;

    /** TEXT | IMAGE | FEED */
    @Field("type")
    private String type;

    /** 텍스트 메시지 내용 또는 이미지/피드 부가 텍스트 */
    @Field("content")
    private String content;

    /** 이미지 타입일 때 이미지 목록 */
    @Field("images")
    private List<ChatImage> images;

    /** 피드 타입일 때 관련 피드 ID */
    @Field("relatedFeedId")
    private Long relatedFeedId;

    @Field("createdAt")
    private Instant createdAt;
}
