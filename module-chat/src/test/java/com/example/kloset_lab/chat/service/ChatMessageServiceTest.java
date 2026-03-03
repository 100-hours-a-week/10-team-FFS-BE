package com.example.kloset_lab.chat.service;

import static com.example.kloset_lab.chat.fixture.ChatFixture.FEED_ID;
import static com.example.kloset_lab.chat.fixture.ChatFixture.MEDIA_FILE_ID;
import static com.example.kloset_lab.chat.fixture.ChatFixture.OPPONENT_ID;
import static com.example.kloset_lab.chat.fixture.ChatFixture.ROOM_ID;
import static com.example.kloset_lab.chat.fixture.ChatFixture.USER_ID;
import static com.example.kloset_lab.chat.fixture.ChatFixture.sendFeedRequest;
import static com.example.kloset_lab.chat.fixture.ChatFixture.sendImageRequest;
import static com.example.kloset_lab.chat.fixture.ChatFixture.sendTextRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;

import com.example.kloset_lab.chat.config.CdnProperties;
import com.example.kloset_lab.chat.constant.ChatConstants;
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
import com.example.kloset_lab.media.entity.MediaFile;
import com.example.kloset_lab.media.entity.Purpose;
import com.example.kloset_lab.media.repository.MediaFileRepository;
import com.example.kloset_lab.media.service.MediaFileConfirmService;
import com.example.kloset_lab.user.entity.User;
import com.example.kloset_lab.user.entity.UserProfile;
import com.example.kloset_lab.user.repository.UserProfileRepository;
import java.util.List;
import java.util.Optional;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

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
    private MediaFileConfirmService mediaFileConfirmService;

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
            willThrow(new CustomException(ErrorCode.TOO_MANY_FILES))
                    .given(mediaFileConfirmService)
                    .confirmFileUpload(USER_ID, Purpose.CHAT, List.of(1L, 2L, 3L, 4L));

            assertCustomException(
                    () -> chatMessageService.sendMessage(USER_ID, ROOM_ID, sendImageRequest(List.of(1L, 2L, 3L, 4L))),
                    ErrorCode.TOO_MANY_FILES);
        }

        @Test
        @DisplayName("IMAGE 파일을 찾지 못하면 FILE_NOT_FOUND 예외")
        void IMAGE_mediaFile_없음_예외() {
            givenParticipantAndRoomFound();
            willThrow(new CustomException(ErrorCode.FILE_NOT_FOUND))
                    .given(mediaFileConfirmService)
                    .confirmFileUpload(USER_ID, Purpose.CHAT, List.of(MEDIA_FILE_ID));

            assertCustomException(
                    () -> chatMessageService.sendMessage(USER_ID, ROOM_ID, sendImageRequest(List.of(MEDIA_FILE_ID))),
                    ErrorCode.FILE_NOT_FOUND);
        }

        @Test
        @DisplayName("IMAGE 파일의 소유자가 다르면 FILE_ACCESS_DENIED 예외")
        void IMAGE_타_사용자_파일_예외() {
            givenParticipantAndRoomFound();
            willThrow(new CustomException(ErrorCode.FILE_ACCESS_DENIED))
                    .given(mediaFileConfirmService)
                    .confirmFileUpload(USER_ID, Purpose.CHAT, List.of(MEDIA_FILE_ID));

            assertCustomException(
                    () -> chatMessageService.sendMessage(USER_ID, ROOM_ID, sendImageRequest(List.of(MEDIA_FILE_ID))),
                    ErrorCode.FILE_ACCESS_DENIED);
        }

        @Test
        @DisplayName("IMAGE 파일 purpose가 CHAT이 아니면 UPLOADED_FILE_MISMATCH 예외")
        void IMAGE_파일_purpose_불일치_예외() {
            givenParticipantAndRoomFound();
            willThrow(new CustomException(ErrorCode.UPLOADED_FILE_MISMATCH))
                    .given(mediaFileConfirmService)
                    .confirmFileUpload(USER_ID, Purpose.CHAT, List.of(MEDIA_FILE_ID));

            assertCustomException(
                    () -> chatMessageService.sendMessage(USER_ID, ROOM_ID, sendImageRequest(List.of(MEDIA_FILE_ID))),
                    ErrorCode.UPLOADED_FILE_MISMATCH);
        }

        @Test
        @DisplayName("IMAGE 파일이 이미 UPLOADED 상태이면 NOT_PENDING_STATE 예외")
        void IMAGE_파일_이미_UPLOADED_NOT_PENDING_STATE_예외() {
            givenParticipantAndRoomFound();
            willThrow(new CustomException(ErrorCode.NOT_PENDING_STATE))
                    .given(mediaFileConfirmService)
                    .confirmFileUpload(USER_ID, Purpose.CHAT, List.of(MEDIA_FILE_ID));

            assertCustomException(
                    () -> chatMessageService.sendMessage(USER_ID, ROOM_ID, sendImageRequest(List.of(MEDIA_FILE_ID))),
                    ErrorCode.NOT_PENDING_STATE);
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
            ReflectionTestUtils.setField(file, "id", MEDIA_FILE_ID);
            givenParticipantAndRoomFound();
            given(mediaFileRepository.findAllById(List.of(MEDIA_FILE_ID))).willReturn(List.of(file));
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

    // ======================== 실패 정책 ========================

    @Nested
    @DisplayName("sendMessage 실패 정책")
    class SendMessageFailurePolicy {

        @Test
        @DisplayName("MongoDB 저장 실패 시 이벤트 미발행 및 ChatRoom 스냅샷 미갱신")
        void Mongo_저장_실패시_후속_단계_중단() {
            ChatRoom room = ChatFixture.chatRoom(ROOM_ID);
            given(chatParticipantRepository.findByRoomIdAndUserId(ROOM_ID, USER_ID))
                    .willReturn(Optional.of(ChatFixture.chatParticipant(room, USER_ID)));
            given(chatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
            given(chatMessageRepository.save(any())).willThrow(new RuntimeException("Mongo 저장 실패"));

            assertThatThrownBy(() -> chatMessageService.sendMessage(USER_ID, ROOM_ID, sendTextRequest("안녕")))
                    .isInstanceOf(RuntimeException.class);

            // 이벤트 미발행 검증
            then(eventPublisher).should(never()).publishEvent(any());
            // ChatRoom 스냅샷 미갱신 검증
            assertThat(room.getLastMessageId()).isNull();
            assertThat(room.getLastMessageContent()).isNull();
        }
    }

    // ======================== 이벤트 페이로드 검증 ========================

    @Nested
    @DisplayName("sendMessage 이벤트 페이로드 검증")
    class SendMessagePayload {

        @Test
        @DisplayName("TEXT 전송 시 이벤트의 contentPreview는 원문 content이고 ChatRoom 스냅샷이 갱신된다")
        void TEXT_이벤트_페이로드_및_스냅샷() {
            ChatRoom room = ChatFixture.chatRoom(ROOM_ID);
            given(chatParticipantRepository.findByRoomIdAndUserId(ROOM_ID, USER_ID))
                    .willReturn(Optional.of(ChatFixture.chatParticipant(room, USER_ID)));
            given(chatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
            given(userProfileRepository.findByUserId(USER_ID)).willReturn(Optional.empty());
            given(chatParticipantRepository.findByRoomId(ROOM_ID)).willReturn(List.of(givenParticipant()));
            given(chatMessageRepository.save(any())).willReturn(ChatFixture.chatMessage(ROOM_ID, USER_ID));

            chatMessageService.sendMessage(USER_ID, ROOM_ID, sendTextRequest("안녕하세요"));

            ArgumentCaptor<ChatMessageSentEvent> captor = ArgumentCaptor.forClass(ChatMessageSentEvent.class);
            then(eventPublisher).should().publishEvent(captor.capture());

            ChatMessageSentEvent event = captor.getValue();
            assertThat(event.roomId()).isEqualTo(ROOM_ID);
            assertThat(event.senderId()).isEqualTo(USER_ID);
            assertThat(event.messageId()).isEqualTo(ChatFixture.VALID_OID);
            assertThat(event.contentPreview()).isEqualTo("안녕하세요");
            assertThat(event.type()).isEqualTo("TEXT");
            assertThat(event.participants()).hasSize(1);
            // ChatRoom 스냅샷 갱신 검증
            assertThat(room.getLastMessageContent()).isEqualTo("안녕하세요");
            assertThat(room.getLastMessageType()).isEqualTo("TEXT");
            assertThat(room.getLastMessageId()).isEqualTo(ChatFixture.VALID_OID);
        }

        @Test
        @DisplayName("IMAGE 전송 시 이벤트의 contentPreview는 [이미지]이다")
        void IMAGE_이벤트_페이로드() {
            User user = ChatFixture.chatUser(USER_ID);
            MediaFile file = ChatFixture.uploadedChatMediaFile(user);
            ReflectionTestUtils.setField(file, "id", MEDIA_FILE_ID);
            givenParticipantAndRoomFound();
            given(mediaFileRepository.findAllById(List.of(MEDIA_FILE_ID))).willReturn(List.of(file));
            given(cdnProperties.getBaseUrl()).willReturn("https://cdn.test.com");
            given(userProfileRepository.findByUserId(USER_ID)).willReturn(Optional.empty());
            given(chatParticipantRepository.findByRoomId(ROOM_ID)).willReturn(List.of(givenParticipant()));
            given(chatMessageRepository.save(any())).willReturn(ChatFixture.chatMessage(ROOM_ID, USER_ID));

            chatMessageService.sendMessage(USER_ID, ROOM_ID, sendImageRequest(List.of(MEDIA_FILE_ID)));

            ArgumentCaptor<ChatMessageSentEvent> captor = ArgumentCaptor.forClass(ChatMessageSentEvent.class);
            then(eventPublisher).should().publishEvent(captor.capture());

            assertThat(captor.getValue().contentPreview()).isEqualTo(ChatConstants.PREVIEW_IMAGE);
            assertThat(captor.getValue().type()).isEqualTo("IMAGE");
        }

        @Test
        @DisplayName("FEED 전송 시 이벤트의 contentPreview는 [피드]이다")
        void FEED_이벤트_페이로드() {
            givenParticipantAndRoomFound();
            given(feedRepository.findById(FEED_ID)).willReturn(Optional.of(mock(Feed.class)));
            given(userProfileRepository.findByUserId(USER_ID)).willReturn(Optional.empty());
            given(chatParticipantRepository.findByRoomId(ROOM_ID)).willReturn(List.of(givenParticipant()));
            given(chatMessageRepository.save(any())).willReturn(ChatFixture.chatMessage(ROOM_ID, USER_ID));

            chatMessageService.sendMessage(USER_ID, ROOM_ID, sendFeedRequest(FEED_ID));

            ArgumentCaptor<ChatMessageSentEvent> captor = ArgumentCaptor.forClass(ChatMessageSentEvent.class);
            then(eventPublisher).should().publishEvent(captor.capture());

            assertThat(captor.getValue().contentPreview()).isEqualTo(ChatConstants.PREVIEW_FEED);
            assertThat(captor.getValue().type()).isEqualTo("FEED");
        }

        @Test
        @DisplayName("UserProfile이 존재하면 broadcastMessage의 senderNickname이 채워진다")
        void senderNickname_설정() {
            UserProfile profile = mock(UserProfile.class);
            given(profile.getNickname()).willReturn("테스터닉");
            givenParticipantAndRoomFound();
            given(userProfileRepository.findByUserId(USER_ID)).willReturn(Optional.of(profile));
            given(chatParticipantRepository.findByRoomId(ROOM_ID)).willReturn(List.of(givenParticipant()));
            given(chatMessageRepository.save(any())).willReturn(ChatFixture.chatMessage(ROOM_ID, USER_ID));

            chatMessageService.sendMessage(USER_ID, ROOM_ID, sendTextRequest("닉네임 테스트"));

            ArgumentCaptor<ChatMessageSentEvent> captor = ArgumentCaptor.forClass(ChatMessageSentEvent.class);
            then(eventPublisher).should().publishEvent(captor.capture());

            assertThat(captor.getValue().broadcastMessage().senderNickname()).isEqualTo("테스터닉");
        }
    }

    // ======================== DM/GROUP 분기 ========================

    @Nested
    @DisplayName("sendMessage DM/GROUP 분기")
    class SendMessageBranching {

        @Test
        @DisplayName("DM 방에서 나간 참여자가 있으면 reenter()가 호출되어 leftAt이 null이 된다")
        void DM_나간_참여자_자동_재진입() {
            // given
            ChatRoom dmRoom = ChatFixture.chatRoom(ROOM_ID);
            given(chatParticipantRepository.findByRoomIdAndUserId(ROOM_ID, USER_ID))
                    .willReturn(Optional.of(ChatFixture.chatParticipant(dmRoom, USER_ID)));
            given(chatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(dmRoom));
            given(userProfileRepository.findByUserId(USER_ID)).willReturn(Optional.empty());
            given(chatMessageRepository.save(any())).willReturn(ChatFixture.chatMessage(ROOM_ID, USER_ID));

            ChatParticipant leftParticipant = ChatFixture.chatParticipant(dmRoom, OPPONENT_ID);
            leftParticipant.leave();
            ChatParticipant activeParticipant = ChatFixture.chatParticipant(dmRoom, USER_ID);
            given(chatParticipantRepository.findByRoomId(ROOM_ID))
                    .willReturn(List.of(activeParticipant, leftParticipant));

            // when
            chatMessageService.sendMessage(USER_ID, ROOM_ID, sendTextRequest("안녕"));

            // then — DM 경로: findByRoomId 호출, findActiveByRoomId 미호출
            then(chatParticipantRepository).should().findByRoomId(ROOM_ID);
            then(chatParticipantRepository).should(never()).findActiveByRoomId(ROOM_ID);
            // 나간 참여자의 leftAt이 null로 초기화됨을 검증
            assertThat(leftParticipant.getLeftAt()).isNull();
        }

        @Test
        @DisplayName("GROUP 방에서는 findActiveByRoomId만 호출하고 findByRoomId는 호출하지 않는다")
        void GROUP_활성_참여자만_조회() {
            // given
            ChatRoom groupRoom = ChatFixture.chatGroupRoom(ROOM_ID);
            given(chatParticipantRepository.findByRoomIdAndUserId(ROOM_ID, USER_ID))
                    .willReturn(Optional.of(ChatFixture.chatParticipant(groupRoom, USER_ID)));
            given(chatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(groupRoom));
            given(userProfileRepository.findByUserId(USER_ID)).willReturn(Optional.empty());
            given(chatMessageRepository.save(any())).willReturn(ChatFixture.chatMessage(ROOM_ID, USER_ID));
            given(chatParticipantRepository.findActiveByRoomId(ROOM_ID))
                    .willReturn(List.of(ChatFixture.chatParticipant(groupRoom, USER_ID)));

            // when
            chatMessageService.sendMessage(USER_ID, ROOM_ID, sendTextRequest("안녕"));

            // then — GROUP 경로: findActiveByRoomId 호출, findByRoomId 미호출
            then(chatParticipantRepository).should().findActiveByRoomId(ROOM_ID);
            then(chatParticipantRepository).should(never()).findByRoomId(ROOM_ID);
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
