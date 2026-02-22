package com.example.kloset_lab.chat.fixture;

import com.example.kloset_lab.chat.document.ChatMessage;
import com.example.kloset_lab.chat.dto.stomp.ChatSendRequest;
import com.example.kloset_lab.chat.entity.ChatParticipant;
import com.example.kloset_lab.chat.entity.ChatRoom;
import com.example.kloset_lab.media.entity.FileStatus;
import com.example.kloset_lab.media.entity.FileType;
import com.example.kloset_lab.media.entity.MediaFile;
import com.example.kloset_lab.media.entity.Purpose;
import com.example.kloset_lab.user.entity.Provider;
import com.example.kloset_lab.user.entity.User;
import java.time.Instant;
import java.util.List;
import org.bson.types.ObjectId;
import org.springframework.test.util.ReflectionTestUtils;

/** 채팅 도메인 테스트 데이터 팩토리 */
public class ChatFixture {

    public static final Long USER_ID = 1L;
    public static final Long OPPONENT_ID = 2L;
    public static final Long ROOM_ID = 10L;
    public static final Long FEED_ID = 100L;
    public static final Long MEDIA_FILE_ID = 50L;

    /** 최신 ObjectId (newer) */
    public static final String VALID_OID = "507f1f77bcf86cd799439011";

    /** VALID_OID보다 오래된 ObjectId (older) */
    public static final String OLDER_OID = "507f1f77bcf86cd799439010";

    /** 유효하지 않은 ObjectId 문자열 */
    public static final String INVALID_OID = "not-an-objectid";

    /** id와 createdAt이 설정된 ChatRoom 생성 */
    public static ChatRoom chatRoom(Long id) {
        ChatRoom room = ChatRoom.builder().id(id).build();
        ReflectionTestUtils.setField(room, "createdAt", Instant.now());
        return room;
    }

    /** ChatParticipant 생성 (enteredAt은 Instant.now()로 자동 설정) */
    public static ChatParticipant chatParticipant(ChatRoom room, Long userId) {
        return ChatParticipant.builder().room(room).userId(userId).build();
    }

    /** 테스트용 User 생성 (id는 ReflectionTestUtils로 설정) */
    public static User chatUser(Long userId) {
        User user = User.builder()
                .provider(Provider.KAKAO)
                .providerId("test-" + userId)
                .build();
        ReflectionTestUtils.setField(user, "id", userId);
        return user;
    }

    /** CHAT purpose + UPLOADED status MediaFile 생성 */
    public static MediaFile uploadedChatMediaFile(User user) {
        MediaFile file = MediaFile.builder()
                .user(user)
                .purpose(Purpose.CHAT)
                .objectKey("chat/test.jpg")
                .fileType(FileType.JPEG)
                .build();
        file.updateFileStatus();
        return file;
    }

    /**
     * purpose·status를 지정할 수 있는 MediaFile 생성
     *
     * @param user    소유자
     * @param purpose 업로드 목적
     * @param status  파일 상태 (UPLOADED이면 updateFileStatus() 호출)
     */
    public static MediaFile mediaFileWith(User user, Purpose purpose, FileStatus status) {
        MediaFile file = MediaFile.builder()
                .user(user)
                .purpose(purpose)
                .objectKey("chat/test.jpg")
                .fileType(FileType.JPEG)
                .build();
        if (status == FileStatus.UPLOADED) {
            file.updateFileStatus();
        }
        return file;
    }

    /** TEXT 메시지 전송 요청 생성 */
    public static ChatSendRequest sendTextRequest(String content) {
        return new ChatSendRequest("TEXT", content, null, null, "client-1");
    }

    /** IMAGE 메시지 전송 요청 생성 */
    public static ChatSendRequest sendImageRequest(List<Long> mediaFileIds) {
        return new ChatSendRequest("IMAGE", null, mediaFileIds, null, "client-1");
    }

    /** FEED 메시지 전송 요청 생성 */
    public static ChatSendRequest sendFeedRequest(Long relatedFeedId) {
        return new ChatSendRequest("FEED", null, null, relatedFeedId, "client-1");
    }

    /** roomId와 senderId가 설정된 ChatMessage 생성 */
    public static ChatMessage chatMessage(Long roomId, Long senderId) {
        return ChatMessage.builder()
                .id(new ObjectId(VALID_OID))
                .roomId(roomId)
                .senderId(senderId)
                .type("TEXT")
                .content("테스트 메시지")
                .createdAt(Instant.now())
                .build();
    }
}
