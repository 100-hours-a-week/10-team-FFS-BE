package com.example.kloset_lab.chat.service;

import static com.example.kloset_lab.chat.fixture.ChatFixture.FEED_ID;
import static com.example.kloset_lab.chat.fixture.ChatFixture.MEDIA_FILE_ID;
import static com.example.kloset_lab.chat.fixture.ChatFixture.ROOM_ID;
import static com.example.kloset_lab.chat.fixture.ChatFixture.USER_ID;
import static com.example.kloset_lab.chat.fixture.ChatFixture.sendFeedRequest;
import static com.example.kloset_lab.chat.fixture.ChatFixture.sendImageRequest;
import static com.example.kloset_lab.chat.fixture.ChatFixture.sendTextRequest;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.example.kloset_lab.chat.config.CdnProperties;
import com.example.kloset_lab.chat.document.ChatMessage;
import com.example.kloset_lab.chat.dto.stomp.ChatSendRequest;
import com.example.kloset_lab.chat.entity.ChatParticipant;
import com.example.kloset_lab.chat.entity.ChatRoom;
import com.example.kloset_lab.chat.event.ChatMessageSentEvent;
import com.example.kloset_lab.chat.fixture.ChatFixture;
import com.example.kloset_lab.chat.repository.ChatMessageRepository;
import com.example.kloset_lab.chat.repository.ChatParticipantRepository;
import com.example.kloset_lab.chat.repository.ChatRoomRepository;
import com.example.kloset_lab.feed.entity.Feed;
import com.example.kloset_lab.feed.repository.FeedRepository;
import com.example.kloset_lab.global.annotation.ServiceTest;
import com.example.kloset_lab.global.exception.CustomException;
import com.example.kloset_lab.global.exception.ErrorCode;
import com.example.kloset_lab.media.entity.FileStatus;
import com.example.kloset_lab.media.entity.MediaFile;
import com.example.kloset_lab.media.entity.Purpose;
import com.example.kloset_lab.media.repository.MediaFileRepository;
import com.example.kloset_lab.user.entity.User;
import com.example.kloset_lab.user.repository.UserProfileRepository;
import java.util.List;
import java.util.Optional;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.ApplicationEventPublisher;

@ServiceTest
@DisplayName("ChatMessageService 단위 테스트")
class ChatMessageServiceTest {

    @Mock
    private ChatParticipantRepository chatParticipantRepository;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private MediaFileRepository mediaFileRepository;

    @Mock
    private FeedRepository feedRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private CdnProperties cdnProperties;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ChatMessageService chatMessageService;

    // ======================== validateMessageType ========================

    @Nested
    @DisplayName("validateMessageType")
    class ValidateMessageType {

        @Test
        @DisplayName("type이 null이면 INVALID_REQUEST 예외")
        void type_null_예외() {
            ChatSendRequest req = new ChatSendRequest(null, null, null, null, "c1");
            assertCustomException(
                    () -> chatMessageService.sendMessage(USER_ID, ROOM_ID, req), ErrorCode.INVALID_REQUEST);
        }

        @Test
        @DisplayName("알 수 없는 type이면 INVALID_REQUEST 예외")
        void 알_수_없는_type_예외() {
            ChatSendRequest req = new ChatSendRequest("UNKNOWN", null, null, null, "c1");
            assertCustomException(
                    () -> chatMessageService.sendMessage(USER_ID, ROOM_ID, req), ErrorCode.INVALID_REQUEST);
        }

        @Test
        @DisplayName("TEXT 타입에 content가 null이면 INVALID_REQUEST 예외")
        void TEXT_content_null_예외() {
            assertCustomException(
                    () -> chatMessageService.sendMessage(USER_ID, ROOM_ID, sendTextRequest(null)),
                    ErrorCode.INVALID_REQUEST);
        }

        @Test
        @DisplayName("TEXT 타입에 content가 공백이면 INVALID_REQUEST 예외")
        void TEXT_content_blank_예외() {
            assertCustomException(
                    () -> chatMessageService.sendMessage(USER_ID, ROOM_ID, sendTextRequest("   ")),
                    ErrorCode.INVALID_REQUEST);
        }

        @Test
        @DisplayName("TEXT 타입에 content가 255자 초과이면 INVALID_REQUEST 예외")
        void TEXT_content_255자_초과_예외() {
            String overLength = "a".repeat(256);
            assertCustomException(
                    () -> chatMessageService.sendMessage(USER_ID, ROOM_ID, sendTextRequest(overLength)),
                    ErrorCode.INVALID_REQUEST);
        }

        @Test
        @DisplayName("IMAGE 타입에 mediaFileIds가 null이면 INVALID_REQUEST 예외")
        void IMAGE_mediaFileIds_null_예외() {
            ChatSendRequest req = new ChatSendRequest("IMAGE", null, null, null, "c1");
            assertCustomException(
                    () -> chatMessageService.sendMessage(USER_ID, ROOM_ID, req), ErrorCode.INVALID_REQUEST);
        }

        @Test
        @DisplayName("IMAGE 타입에 mediaFileIds가 빈 리스트이면 INVALID_REQUEST 예외")
        void IMAGE_mediaFileIds_empty_예외() {
            ChatSendRequest req = new ChatSendRequest("IMAGE", null, List.of(), null, "c1");
            assertCustomException(
                    () -> chatMessageService.sendMessage(USER_ID, ROOM_ID, req), ErrorCode.INVALID_REQUEST);
        }

        @Test
        @DisplayName("FEED 타입에 relatedFeedId가 null이면 INVALID_REQUEST 예외")
        void FEED_relatedFeedId_null_예외() {
            ChatSendRequest req = new ChatSendRequest("FEED", null, null, null, "c1");
            assertCustomException(
                    () -> chatMessageService.sendMessage(USER_ID, ROOM_ID, req), ErrorCode.INVALID_REQUEST);
        }

        @Test
        @DisplayName("TEXT 타입에 content가 유효하면 validateMessageType을 통과해 참여자 조회를 실행한다")
        void TEXT_content_유효_검증_통과() {
            given(chatParticipantRepository.findByRoomIdAndUserId(ROOM_ID, USER_ID))
                    .willReturn(Optional.empty());

            // 검증 통과 후 참여자 조회에서 CHAT_ROOM_ACCESS_DENIED → INVALID_REQUEST가 아님을 확인
            assertCustomException(
                    () -> chatMessageService.sendMessage(USER_ID, ROOM_ID, sendTextRequest("안녕하세요")),
                    ErrorCode.CHAT_ROOM_ACCESS_DENIED);
            then(chatParticipantRepository).should().findByRoomIdAndUserId(ROOM_ID, USER_ID);
        }
    }

    // ======================== sendMessage 권한·비즈니스 검증 ========================

    @Nested
    @DisplayName("sendMessage 권한·비즈니스 검증")
    class SendMessageValidation {

        @Test
        @DisplayName("참여자가 아닌 경우 CHAT_ROOM_ACCESS_DENIED 예외")
        void 참여자_아님_예외() {
            given(chatParticipantRepository.findByRoomIdAndUserId(ROOM_ID, USER_ID))
                    .willReturn(Optional.empty());

            assertCustomException(
                    () -> chatMessageService.sendMessage(USER_ID, ROOM_ID, sendTextRequest("hello")),
                    ErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }

        @Test
        @DisplayName("채팅방이 존재하지 않으면 CHAT_ROOM_NOT_FOUND 예외")
        void 채팅방_없음_예외() {
            givenParticipantFound();
            given(chatRoomRepository.findById(ROOM_ID)).willReturn(Optional.empty());

            assertCustomException(
                    () -> chatMessageService.sendMessage(USER_ID, ROOM_ID, sendTextRequest("hello")),
                    ErrorCode.CHAT_ROOM_NOT_FOUND);
        }

        @Test
        @DisplayName("IMAGE 파일이 maxCount(3)를 초과하면 TOO_MANY_FILES 예외")
        void IMAGE_파일_수_초과_예외() {
            givenParticipantAndRoomFound();

            assertCustomException(
                    () -> chatMessageService.sendMessage(USER_ID, ROOM_ID, sendImageRequest(List.of(1L, 2L, 3L, 4L))),
                    ErrorCode.TOO_MANY_FILES);
        }

        @Test
        @DisplayName("IMAGE 파일을 찾지 못하면 FILE_NOT_FOUND 예외")
        void IMAGE_mediaFile_없음_예외() {
            givenParticipantAndRoomFound();
            given(mediaFileRepository.findById(MEDIA_FILE_ID)).willReturn(Optional.empty());

            assertCustomException(
                    () -> chatMessageService.sendMessage(USER_ID, ROOM_ID, sendImageRequest(List.of(MEDIA_FILE_ID))),
                    ErrorCode.FILE_NOT_FOUND);
        }

        @Test
        @DisplayName("IMAGE 파일의 소유자가 다르면 FILE_ACCESS_DENIED 예외")
        void IMAGE_타_사용자_파일_예외() {
            User otherUser = ChatFixture.chatUser(99L);
            MediaFile file = ChatFixture.uploadedChatMediaFile(otherUser);
            givenParticipantAndRoomFound();
            given(mediaFileRepository.findById(MEDIA_FILE_ID)).willReturn(Optional.of(file));

            assertCustomException(
                    () -> chatMessageService.sendMessage(USER_ID, ROOM_ID, sendImageRequest(List.of(MEDIA_FILE_ID))),
                    ErrorCode.FILE_ACCESS_DENIED);
        }

        @Test
        @DisplayName("IMAGE 파일 purpose가 CHAT이 아니면 INVALID_REQUEST 예외")
        void IMAGE_파일_purpose_불일치_예외() {
            User user = ChatFixture.chatUser(USER_ID);
            MediaFile file = ChatFixture.mediaFileWith(user, Purpose.FEED, FileStatus.UPLOADED);
            givenParticipantAndRoomFound();
            given(mediaFileRepository.findById(MEDIA_FILE_ID)).willReturn(Optional.of(file));

            assertCustomException(
                    () -> chatMessageService.sendMessage(USER_ID, ROOM_ID, sendImageRequest(List.of(MEDIA_FILE_ID))),
                    ErrorCode.INVALID_REQUEST);
        }

        @Test
        @DisplayName("IMAGE 파일 status가 UPLOADED가 아니면 INVALID_REQUEST 예외")
        void IMAGE_파일_status_PENDING_예외() {
            User user = ChatFixture.chatUser(USER_ID);
            MediaFile file = ChatFixture.mediaFileWith(user, Purpose.CHAT, FileStatus.PENDING);
            givenParticipantAndRoomFound();
            given(mediaFileRepository.findById(MEDIA_FILE_ID)).willReturn(Optional.of(file));

            assertCustomException(
                    () -> chatMessageService.sendMessage(USER_ID, ROOM_ID, sendImageRequest(List.of(MEDIA_FILE_ID))),
                    ErrorCode.INVALID_REQUEST);
        }

        @Test
        @DisplayName("FEED 피드가 존재하지 않으면 FEED_NOT_FOUND 예외")
        void FEED_피드_없음_예외() {
            givenParticipantAndRoomFound();
            given(feedRepository.findById(FEED_ID)).willReturn(Optional.empty());

            assertCustomException(
                    () -> chatMessageService.sendMessage(USER_ID, ROOM_ID, sendFeedRequest(FEED_ID)),
                    ErrorCode.FEED_NOT_FOUND);
        }
    }

    // ======================== sendMessage 정상 전송 ========================

    @Nested
    @DisplayName("sendMessage 정상 전송")
    class SendMessageSuccess {

        @Test
        @DisplayName("TEXT 메시지 전송 시 MongoDB에 저장하고 이벤트를 발행한다")
        void TEXT_정상_전송() {
            givenParticipantAndRoomFound();
            given(userProfileRepository.findByUserId(USER_ID)).willReturn(Optional.empty());
            given(chatParticipantRepository.findByRoomId(ROOM_ID)).willReturn(List.of(givenParticipant()));
            given(chatMessageRepository.save(any())).willReturn(ChatFixture.chatMessage(ROOM_ID, USER_ID));

            chatMessageService.sendMessage(USER_ID, ROOM_ID, sendTextRequest("안녕하세요"));

            then(chatMessageRepository).should().save(any(ChatMessage.class));
            then(eventPublisher).should().publishEvent(any(ChatMessageSentEvent.class));
        }

        @Test
        @DisplayName("IMAGE 메시지 전송 시 이미지 정보가 포함된 ChatMessage를 저장한다")
        void IMAGE_정상_전송() {
            User user = ChatFixture.chatUser(USER_ID);
            MediaFile file = ChatFixture.uploadedChatMediaFile(user);
            givenParticipantAndRoomFound();
            given(mediaFileRepository.findById(MEDIA_FILE_ID)).willReturn(Optional.of(file));
            given(cdnProperties.getBaseUrl()).willReturn("https://cdn.test.com");
            given(userProfileRepository.findByUserId(USER_ID)).willReturn(Optional.empty());
            given(chatParticipantRepository.findByRoomId(ROOM_ID)).willReturn(List.of(givenParticipant()));
            given(chatMessageRepository.save(any())).willReturn(ChatFixture.chatMessage(ROOM_ID, USER_ID));

            chatMessageService.sendMessage(USER_ID, ROOM_ID, sendImageRequest(List.of(MEDIA_FILE_ID)));

            then(chatMessageRepository).should().save(any(ChatMessage.class));
            then(eventPublisher).should().publishEvent(any(ChatMessageSentEvent.class));
        }

        @Test
        @DisplayName("FEED 메시지 전송 시 feedId가 포함된 ChatMessage를 저장한다")
        void FEED_정상_전송() {
            givenParticipantAndRoomFound();
            given(feedRepository.findById(FEED_ID)).willReturn(Optional.of(mock(Feed.class)));
            given(userProfileRepository.findByUserId(USER_ID)).willReturn(Optional.empty());
            given(chatParticipantRepository.findByRoomId(ROOM_ID)).willReturn(List.of(givenParticipant()));
            given(chatMessageRepository.save(any())).willReturn(ChatFixture.chatMessage(ROOM_ID, USER_ID));

            chatMessageService.sendMessage(USER_ID, ROOM_ID, sendFeedRequest(FEED_ID));

            then(chatMessageRepository).should().save(any(ChatMessage.class));
            then(eventPublisher).should().publishEvent(any(ChatMessageSentEvent.class));
        }
    }

    // ======================== 헬퍼 ========================

    private ChatParticipant givenParticipant() {
        ChatRoom room = ChatFixture.chatRoom(ROOM_ID);
        return ChatFixture.chatParticipant(room, USER_ID);
    }

    private void givenParticipantFound() {
        given(chatParticipantRepository.findByRoomIdAndUserId(ROOM_ID, USER_ID))
                .willReturn(Optional.of(givenParticipant()));
    }

    private void givenParticipantAndRoomFound() {
        givenParticipantFound();
        given(chatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(ChatFixture.chatRoom(ROOM_ID)));
    }

    private void assertCustomException(ThrowingCallable callable, ErrorCode expectedCode) {
        assertThatThrownBy(callable)
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(expectedCode);
    }
}
