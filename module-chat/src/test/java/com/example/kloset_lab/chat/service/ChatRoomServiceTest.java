package com.example.kloset_lab.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoInteractions;

import com.example.kloset_lab.chat.config.CdnProperties;
import com.example.kloset_lab.chat.document.ChatMessage;
import com.example.kloset_lab.chat.dto.ChatRoomCreateRequest;
import com.example.kloset_lab.chat.dto.ChatRoomListResponse;
import com.example.kloset_lab.chat.dto.ChatRoomResult;
import com.example.kloset_lab.chat.dto.ReadRequest;
import com.example.kloset_lab.chat.dto.UnreadStatusResponse;
import com.example.kloset_lab.chat.entity.ChatParticipant;
import com.example.kloset_lab.chat.entity.ChatRoom;
import com.example.kloset_lab.chat.event.ChatParticipantLeftEvent;
import com.example.kloset_lab.chat.event.ChatReadEvent;
import com.example.kloset_lab.chat.event.ChatRoomCreatedEvent;
import com.example.kloset_lab.chat.event.ChatRoomDeletedEvent;
import com.example.kloset_lab.chat.fixture.ChatFixture;
import com.example.kloset_lab.chat.infrastructure.ChatRedisRepository;
import com.example.kloset_lab.chat.repository.ChatMessageRepository;
import com.example.kloset_lab.chat.repository.ChatParticipantRepository;
import com.example.kloset_lab.chat.repository.ChatRoomRepository;
import com.example.kloset_lab.global.annotation.ServiceTest;
import com.example.kloset_lab.global.exception.CustomException;
import com.example.kloset_lab.global.exception.ErrorCode;
import com.example.kloset_lab.user.repository.UserProfileRepository;
import com.example.kloset_lab.user.repository.UserRepository;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@ServiceTest
@DisplayName("ChatRoomService 단위 테스트")
class ChatRoomServiceTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatParticipantRepository chatParticipantRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private ChatRedisRepository chatRedisRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private CdnProperties cdnProperties;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ChatRoomService chatRoomService;

    // ======================== createOrGetRoom ========================

    @Nested
    @DisplayName("createOrGetRoom")
    class CreateOrGetRoom {

        @Test
        @DisplayName("자기 자신과 채팅방 생성 시 CANNOT_CHAT_WITH_SELF 예외")
        void 자기_자신과_채팅_예외() {
            ChatRoomCreateRequest req = new ChatRoomCreateRequest(ChatFixture.USER_ID);

            assertCustomException(
                    () -> chatRoomService.createOrGetRoom(ChatFixture.USER_ID, req), ErrorCode.CANNOT_CHAT_WITH_SELF);
        }

        @Test
        @DisplayName("상대방이 존재하지 않으면 TARGET_USER_NOT_FOUND 예외")
        void 상대방_없음_예외() {
            ChatRoomCreateRequest req = new ChatRoomCreateRequest(ChatFixture.OPPONENT_ID);
            given(userRepository.findById(ChatFixture.USER_ID))
                    .willReturn(Optional.of(ChatFixture.chatUser(ChatFixture.USER_ID)));
            given(userRepository.findById(ChatFixture.OPPONENT_ID)).willReturn(Optional.empty());

            assertCustomException(
                    () -> chatRoomService.createOrGetRoom(ChatFixture.USER_ID, req), ErrorCode.TARGET_USER_NOT_FOUND);
        }

        @Test
        @DisplayName("기존 방이 있고 참여자가 활성이면 재진입 처리 없이 existing() 반환")
        void 기존_방_참여자_존재_기존_반환() {
            ChatRoom room = ChatFixture.chatRoom(ChatFixture.ROOM_ID);
            // leftAt = null → 활성 참여자
            ChatParticipant participant = ChatFixture.chatParticipant(room, ChatFixture.USER_ID);
            ChatRoomCreateRequest req = new ChatRoomCreateRequest(ChatFixture.OPPONENT_ID);

            given(userRepository.findById(ChatFixture.USER_ID))
                    .willReturn(Optional.of(ChatFixture.chatUser(ChatFixture.USER_ID)));
            given(userRepository.findById(ChatFixture.OPPONENT_ID))
                    .willReturn(Optional.of(ChatFixture.chatUser(ChatFixture.OPPONENT_ID)));
            given(chatRoomRepository.findExistingRoomBetweenUsers(ChatFixture.USER_ID, ChatFixture.OPPONENT_ID))
                    .willReturn(Optional.of(room));
            given(chatParticipantRepository.findHistoryByRoomIdAndUserId(ChatFixture.ROOM_ID, ChatFixture.USER_ID))
                    .willReturn(Optional.of(participant));
            given(userProfileRepository.findByUserId(ChatFixture.OPPONENT_ID)).willReturn(Optional.empty());

            ChatRoomResult result = chatRoomService.createOrGetRoom(ChatFixture.USER_ID, req);

            assertThat(result.created()).isFalse();
            // 활성 참여자이므로 save 호출 없음
            then(chatParticipantRepository).should(never()).save(any());
            then(eventPublisher).should(never()).publishEvent(any(ChatRoomCreatedEvent.class));
        }

        @Test
        @DisplayName("기존 방이 있고 참여자가 이전에 나갔으면 reenter() 처리 후 existing() 반환")
        void 기존_방_참여자_없음_재진입() {
            ChatRoom room = ChatFixture.chatRoom(ChatFixture.ROOM_ID);
            // leftAt 설정 → 나간 참여자
            ChatParticipant leftParticipant = ChatFixture.chatParticipant(room, ChatFixture.USER_ID);
            leftParticipant.leave();
            ChatRoomCreateRequest req = new ChatRoomCreateRequest(ChatFixture.OPPONENT_ID);

            given(userRepository.findById(ChatFixture.USER_ID))
                    .willReturn(Optional.of(ChatFixture.chatUser(ChatFixture.USER_ID)));
            given(userRepository.findById(ChatFixture.OPPONENT_ID))
                    .willReturn(Optional.of(ChatFixture.chatUser(ChatFixture.OPPONENT_ID)));
            given(chatRoomRepository.findExistingRoomBetweenUsers(ChatFixture.USER_ID, ChatFixture.OPPONENT_ID))
                    .willReturn(Optional.of(room));
            given(chatParticipantRepository.findHistoryByRoomIdAndUserId(ChatFixture.ROOM_ID, ChatFixture.USER_ID))
                    .willReturn(Optional.of(leftParticipant));
            given(userProfileRepository.findByUserId(ChatFixture.OPPONENT_ID)).willReturn(Optional.empty());

            ChatRoomResult result = chatRoomService.createOrGetRoom(ChatFixture.USER_ID, req);

            assertThat(result.created()).isFalse();
            // reenter() 호출로 leftAt이 null로 초기화됨
            assertThat(leftParticipant.getLeftAt()).isNull();
            // save 없이 dirty checking으로 처리, 이벤트 발행
            then(chatParticipantRepository).should(never()).save(any());
            then(eventPublisher).should().publishEvent(any(ChatRoomCreatedEvent.class));
        }

        @Test
        @DisplayName("기존 방이 없으면 새 방을 생성하고 created() 반환")
        void 기존_방_없음_신규_생성() {
            ChatRoomCreateRequest req = new ChatRoomCreateRequest(ChatFixture.OPPONENT_ID);

            given(userRepository.findById(ChatFixture.USER_ID))
                    .willReturn(Optional.of(ChatFixture.chatUser(ChatFixture.USER_ID)));
            given(userRepository.findById(ChatFixture.OPPONENT_ID))
                    .willReturn(Optional.of(ChatFixture.chatUser(ChatFixture.OPPONENT_ID)));
            given(chatRoomRepository.findExistingRoomBetweenUsers(ChatFixture.USER_ID, ChatFixture.OPPONENT_ID))
                    .willReturn(Optional.empty());
            given(chatRoomRepository.save(any())).willAnswer(inv -> {
                ChatRoom saved = inv.getArgument(0);
                ReflectionTestUtils.setField(saved, "id", ChatFixture.ROOM_ID);
                ReflectionTestUtils.setField(saved, "createdAt", Instant.now());
                return saved;
            });
            given(chatParticipantRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(userProfileRepository.findByUserId(ChatFixture.OPPONENT_ID)).willReturn(Optional.empty());

            ChatRoomResult result = chatRoomService.createOrGetRoom(ChatFixture.USER_ID, req);

            assertThat(result.created()).isTrue();
            then(chatParticipantRepository).should(times(2)).save(any(ChatParticipant.class));
            then(eventPublisher).should().publishEvent(any(ChatRoomCreatedEvent.class));
        }
    }

    // ======================== getRooms ========================

    @Nested
    @DisplayName("getRooms")
    class GetRooms {

        @Test
        @DisplayName("Redis 캐시가 비어 있으면 rebuildRoomCache를 호출한다")
        void Redis_캐시_없음_재구축_호출() {
            given(chatRedisRepository.getRoomCount(ChatFixture.USER_ID)).willReturn(0L);
            given(chatParticipantRepository.findByUserId(ChatFixture.USER_ID)).willReturn(List.of());
            given(chatRedisRepository.getRoomsDescWithScores(eq(ChatFixture.USER_ID), eq(Double.MAX_VALUE), eq(11)))
                    .willReturn(Collections.emptyMap());

            chatRoomService.getRooms(ChatFixture.USER_ID, null, 10);

            then(chatParticipantRepository).should().findByUserId(ChatFixture.USER_ID);
        }

        @Test
        @DisplayName("cursor가 없으면 Double.MAX_VALUE로 조회한다")
        void cursor_없으면_MAX_VALUE_사용() {
            given(chatRedisRepository.getRoomCount(ChatFixture.USER_ID)).willReturn(1L);
            given(chatRedisRepository.getRoomsDescWithScores(eq(ChatFixture.USER_ID), eq(Double.MAX_VALUE), eq(11)))
                    .willReturn(Collections.emptyMap());

            chatRoomService.getRooms(ChatFixture.USER_ID, null, 10);

            then(chatRedisRepository).should().getRoomsDescWithScores(ChatFixture.USER_ID, Double.MAX_VALUE, 11);
        }

        @Test
        @DisplayName("cursor가 있으면 cursor - 1.0 적용하여 중복 조회를 방지한다")
        void cursor_있으면_minus1_적용() {
            double cursor = 1_700_000_000_000.0;
            given(chatRedisRepository.getRoomCount(ChatFixture.USER_ID)).willReturn(1L);
            given(chatRedisRepository.getRoomsDescWithScores(eq(ChatFixture.USER_ID), eq(cursor - 1.0), eq(11)))
                    .willReturn(Collections.emptyMap());

            chatRoomService.getRooms(ChatFixture.USER_ID, cursor, 10);

            then(chatRedisRepository).should().getRoomsDescWithScores(ChatFixture.USER_ID, cursor - 1.0, 11);
        }

        @Test
        @DisplayName("size + 1개가 반환되면 hasNextPage = true")
        void size_plus1_반환시_hasNextPage_true() {
            int size = 2;
            given(chatRedisRepository.getRoomCount(ChatFixture.USER_ID)).willReturn(1L);
            // size+1 = 3개를 반환 → 다음 페이지 존재
            Map<String, Double> roomScores = new LinkedHashMap<>();
            roomScores.put("10", 1000.0);
            roomScores.put("20", 900.0);
            roomScores.put("30", 800.0);
            given(chatRedisRepository.getRoomsDescWithScores(eq(ChatFixture.USER_ID), eq(Double.MAX_VALUE), eq(size + 1)))
                    .willReturn(roomScores);
            given(chatParticipantRepository.findOpponentsByRoomIds(anyList(), eq(ChatFixture.USER_ID)))
                    .willReturn(List.of());
            given(chatRedisRepository.getLastMessagesBatch(anyList())).willReturn(Collections.emptyMap());
            given(chatRedisRepository.getUnreadBatch(eq(ChatFixture.USER_ID), anyList()))
                    .willReturn(Collections.emptyMap());

            ChatRoomListResponse response = chatRoomService.getRooms(ChatFixture.USER_ID, null, size);

            assertThat(response.hasNextPage()).isTrue();
        }
    }

    // ======================== leaveRoom ========================

    @Nested
    @DisplayName("leaveRoom")
    class LeaveRoom {

        @Test
        @DisplayName("참여자가 아닌 경우 CHAT_ROOM_ACCESS_DENIED 예외")
        void 참여자_아님_예외() {
            given(chatParticipantRepository.findByRoomIdAndUserId(ChatFixture.ROOM_ID, ChatFixture.USER_ID))
                    .willReturn(Optional.empty());

            assertCustomException(
                    () -> chatRoomService.leaveRoom(ChatFixture.USER_ID, ChatFixture.ROOM_ID),
                    ErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }

        @Test
        @DisplayName("나간 후 참여자가 남아 있으면 채팅방 soft delete를 수행하지 않는다")
        void 나간_후_참여자_남음_soft_delete_없음() {
            ChatRoom room = ChatFixture.chatRoom(ChatFixture.ROOM_ID);
            ChatParticipant participant = ChatFixture.chatParticipant(room, ChatFixture.USER_ID);
            given(chatParticipantRepository.findByRoomIdAndUserId(ChatFixture.ROOM_ID, ChatFixture.USER_ID))
                    .willReturn(Optional.of(participant));
            given(chatParticipantRepository.countByRoomId(ChatFixture.ROOM_ID)).willReturn(1L);

            chatRoomService.leaveRoom(ChatFixture.USER_ID, ChatFixture.ROOM_ID);

            // leave() 호출로 leftAt이 설정됨 (hard delete 아닌 soft delete)
            assertThat(participant.getLeftAt()).isNotNull();
            then(eventPublisher).should().publishEvent(any(ChatParticipantLeftEvent.class));
            then(chatRoomRepository).should(never()).findById(anyLong());
        }

        @Test
        @DisplayName("나간 후 참여자가 0명이면 채팅방 soft delete를 수행한다")
        void 나간_후_참여자_0명_soft_delete_수행() {
            ChatRoom room = ChatFixture.chatRoom(ChatFixture.ROOM_ID);
            ChatParticipant participant = ChatFixture.chatParticipant(room, ChatFixture.USER_ID);
            given(chatParticipantRepository.findByRoomIdAndUserId(ChatFixture.ROOM_ID, ChatFixture.USER_ID))
                    .willReturn(Optional.of(participant));
            given(chatParticipantRepository.countByRoomId(ChatFixture.ROOM_ID)).willReturn(0L);
            given(chatRoomRepository.findById(ChatFixture.ROOM_ID)).willReturn(Optional.of(room));

            chatRoomService.leaveRoom(ChatFixture.USER_ID, ChatFixture.ROOM_ID);

            assertThat(participant.getLeftAt()).isNotNull();
            assertThat(room.isDeleted()).isTrue();
            then(eventPublisher).should().publishEvent(any(ChatRoomDeletedEvent.class));
        }

        @Test
        @DisplayName("soft delete 시 채팅방을 찾지 못하면 CHAT_ROOM_NOT_FOUND 예외")
        void soft_delete_시_방_없음_예외() {
            ChatRoom room = ChatFixture.chatRoom(ChatFixture.ROOM_ID);
            ChatParticipant participant = ChatFixture.chatParticipant(room, ChatFixture.USER_ID);
            given(chatParticipantRepository.findByRoomIdAndUserId(ChatFixture.ROOM_ID, ChatFixture.USER_ID))
                    .willReturn(Optional.of(participant));
            given(chatParticipantRepository.countByRoomId(ChatFixture.ROOM_ID)).willReturn(0L);
            given(chatRoomRepository.findById(ChatFixture.ROOM_ID)).willReturn(Optional.empty());

            assertCustomException(
                    () -> chatRoomService.leaveRoom(ChatFixture.USER_ID, ChatFixture.ROOM_ID),
                    ErrorCode.CHAT_ROOM_NOT_FOUND);
        }
    }

    // ======================== markAsRead ========================

    @Nested
    @DisplayName("markAsRead")
    class MarkAsRead {

        @Test
        @DisplayName("유효하지 않은 ObjectId 형식이면 INVALID_REQUEST 예외 + 참여자 조회 없음")
        void 유효하지_않은_ObjectId_예외() {
            ReadRequest req = new ReadRequest(ChatFixture.INVALID_OID);

            assertCustomException(
                    () -> chatRoomService.markAsRead(ChatFixture.USER_ID, ChatFixture.ROOM_ID, req),
                    ErrorCode.INVALID_REQUEST);
            verifyNoInteractions(chatParticipantRepository);
        }

        @Test
        @DisplayName("참여자가 아닌 경우 CHAT_ROOM_ACCESS_DENIED 예외")
        void 참여자_아님_예외() {
            given(chatParticipantRepository.findByRoomIdAndUserId(ChatFixture.ROOM_ID, ChatFixture.USER_ID))
                    .willReturn(Optional.empty());

            assertCustomException(
                    () -> chatRoomService.markAsRead(
                            ChatFixture.USER_ID, ChatFixture.ROOM_ID, new ReadRequest(ChatFixture.VALID_OID)),
                    ErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }

        @Test
        @DisplayName("새 messageId가 현재보다 최신이면 lastReadMessageId 갱신 및 이벤트 발행")
        void 새_messageId_최신_갱신_이벤트_발행() {
            ChatRoom room = ChatFixture.chatRoom(ChatFixture.ROOM_ID);
            ChatParticipant participant = ChatFixture.chatParticipant(room, ChatFixture.USER_ID);
            participant.updateLastReadMessageId(ChatFixture.OLDER_OID); // 현재는 구 버전 ID
            given(chatParticipantRepository.findByRoomIdAndUserId(ChatFixture.ROOM_ID, ChatFixture.USER_ID))
                    .willReturn(Optional.of(participant));

            chatRoomService.markAsRead(
                    ChatFixture.USER_ID, ChatFixture.ROOM_ID, new ReadRequest(ChatFixture.VALID_OID)); // 최신 ID로 갱신 요청

            assertThat(participant.getLastReadMessageId()).isEqualTo(ChatFixture.VALID_OID);
            then(eventPublisher).should().publishEvent(any(ChatReadEvent.class));
        }

        @Test
        @DisplayName("새 messageId가 현재보다 오래됐으면 lastReadMessageId는 갱신하지 않지만 unread reset 이벤트는 항상 발행한다")
        void 새_messageId_오래됨_lastRead_갱신_없음_unread_reset_발행() {
            ChatRoom room = ChatFixture.chatRoom(ChatFixture.ROOM_ID);
            ChatParticipant participant = ChatFixture.chatParticipant(room, ChatFixture.USER_ID);
            participant.updateLastReadMessageId(ChatFixture.VALID_OID); // 현재는 최신 ID
            given(chatParticipantRepository.findByRoomIdAndUserId(ChatFixture.ROOM_ID, ChatFixture.USER_ID))
                    .willReturn(Optional.of(participant));

            chatRoomService.markAsRead(
                    ChatFixture.USER_ID, ChatFixture.ROOM_ID, new ReadRequest(ChatFixture.OLDER_OID)); // 오래된 ID로 갱신 요청

            assertThat(participant.getLastReadMessageId())
                    .isEqualTo(ChatFixture.VALID_OID); // lastReadMessageId 역방향 갱신 방지
            then(eventPublisher).should().publishEvent(any(ChatReadEvent.class)); // unread reset은 항상 수행
        }
    }

    // ======================== getMessages ========================

    @Nested
    @DisplayName("getMessages")
    class GetMessages {

        @Test
        @DisplayName("참여자가 아닌 경우 CHAT_ROOM_ACCESS_DENIED 예외")
        void 참여자_아님_예외() {
            given(chatParticipantRepository.findByRoomIdAndUserId(ChatFixture.ROOM_ID, ChatFixture.USER_ID))
                    .willReturn(Optional.empty());

            assertCustomException(
                    () -> chatRoomService.getMessages(ChatFixture.USER_ID, ChatFixture.ROOM_ID, null, 20),
                    ErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }

        @Test
        @DisplayName("cursor가 null이면 enteredAt 이후 전체 메시지를 조회한다")
        void cursor_null_enteredAt_이후_전체_조회() {
            ChatRoom room = ChatFixture.chatRoom(ChatFixture.ROOM_ID);
            ChatParticipant participant = ChatFixture.chatParticipant(room, ChatFixture.USER_ID);
            given(chatParticipantRepository.findByRoomIdAndUserId(ChatFixture.ROOM_ID, ChatFixture.USER_ID))
                    .willReturn(Optional.of(participant));
            given(chatMessageRepository.findByRoomIdAndCreatedAtGreaterThanEqual(
                            eq(ChatFixture.ROOM_ID), any(Instant.class), any()))
                    .willReturn(List.of());

            chatRoomService.getMessages(ChatFixture.USER_ID, ChatFixture.ROOM_ID, null, 20);

            then(chatMessageRepository)
                    .should()
                    .findByRoomIdAndCreatedAtGreaterThanEqual(eq(ChatFixture.ROOM_ID), any(Instant.class), any());
        }

        @Test
        @DisplayName("유효한 cursor면 _id < cursor 조건 커서 페이지네이션으로 조회한다")
        void 유효한_cursor_커서_페이지네이션_조회() {
            ChatRoom room = ChatFixture.chatRoom(ChatFixture.ROOM_ID);
            ChatParticipant participant = ChatFixture.chatParticipant(room, ChatFixture.USER_ID);
            given(chatParticipantRepository.findByRoomIdAndUserId(ChatFixture.ROOM_ID, ChatFixture.USER_ID))
                    .willReturn(Optional.of(participant));
            given(chatMessageRepository.findByRoomIdAndIdLessThanAndCreatedAtGreaterThanEqual(
                            eq(ChatFixture.ROOM_ID), any(ObjectId.class), any(Instant.class), any()))
                    .willReturn(List.of());

            chatRoomService.getMessages(ChatFixture.USER_ID, ChatFixture.ROOM_ID, ChatFixture.VALID_OID, 20);

            then(chatMessageRepository)
                    .should()
                    .findByRoomIdAndIdLessThanAndCreatedAtGreaterThanEqual(
                            eq(ChatFixture.ROOM_ID), any(ObjectId.class), any(Instant.class), any());
        }

        @Test
        @DisplayName("cursor가 유효하지 않은 ObjectId면 전체 조회로 폴백한다")
        void 유효하지_않은_cursor_전체_조회_폴백() {
            ChatRoom room = ChatFixture.chatRoom(ChatFixture.ROOM_ID);
            ChatParticipant participant = ChatFixture.chatParticipant(room, ChatFixture.USER_ID);
            given(chatParticipantRepository.findByRoomIdAndUserId(ChatFixture.ROOM_ID, ChatFixture.USER_ID))
                    .willReturn(Optional.of(participant));
            given(chatMessageRepository.findByRoomIdAndCreatedAtGreaterThanEqual(
                            eq(ChatFixture.ROOM_ID), any(Instant.class), any()))
                    .willReturn(List.of());

            chatRoomService.getMessages(ChatFixture.USER_ID, ChatFixture.ROOM_ID, ChatFixture.INVALID_OID, 20);

            then(chatMessageRepository)
                    .should()
                    .findByRoomIdAndCreatedAtGreaterThanEqual(eq(ChatFixture.ROOM_ID), any(Instant.class), any());
        }

        @Test
        @DisplayName("메시지가 있으면 읽음 처리 이벤트를 발행한다")
        void 메시지_있으면_읽음_이벤트_발행() {
            ChatRoom room = ChatFixture.chatRoom(ChatFixture.ROOM_ID);
            ChatParticipant participant = ChatFixture.chatParticipant(room, ChatFixture.USER_ID);
            ChatMessage message = ChatFixture.chatMessage(ChatFixture.ROOM_ID, ChatFixture.USER_ID);
            given(chatParticipantRepository.findByRoomIdAndUserId(ChatFixture.ROOM_ID, ChatFixture.USER_ID))
                    .willReturn(Optional.of(participant));
            given(chatMessageRepository.findByRoomIdAndCreatedAtGreaterThanEqual(
                            eq(ChatFixture.ROOM_ID), any(Instant.class), any()))
                    .willReturn(List.of(message));

            chatRoomService.getMessages(ChatFixture.USER_ID, ChatFixture.ROOM_ID, null, 20);

            then(eventPublisher).should().publishEvent(any(ChatReadEvent.class));
        }

        @Test
        @DisplayName("메시지가 없으면 읽음 처리 이벤트를 발행하지 않는다")
        void 메시지_없으면_읽음_이벤트_미발행() {
            ChatRoom room = ChatFixture.chatRoom(ChatFixture.ROOM_ID);
            ChatParticipant participant = ChatFixture.chatParticipant(room, ChatFixture.USER_ID);
            given(chatParticipantRepository.findByRoomIdAndUserId(ChatFixture.ROOM_ID, ChatFixture.USER_ID))
                    .willReturn(Optional.of(participant));
            given(chatMessageRepository.findByRoomIdAndCreatedAtGreaterThanEqual(
                            eq(ChatFixture.ROOM_ID), any(Instant.class), any()))
                    .willReturn(List.of());

            chatRoomService.getMessages(ChatFixture.USER_ID, ChatFixture.ROOM_ID, null, 20);

            verifyNoInteractions(eventPublisher);
        }
    }

    // ======================== getUnreadMessages ========================

    @Nested
    @DisplayName("getUnreadMessages")
    class GetUnreadMessages {

        @Test
        @DisplayName("참여자가 아닌 경우 CHAT_ROOM_ACCESS_DENIED 예외")
        void 참여자_아님_예외() {
            given(chatParticipantRepository.findByRoomIdAndUserId(ChatFixture.ROOM_ID, ChatFixture.USER_ID))
                    .willReturn(Optional.empty());

            assertCustomException(
                    () -> chatRoomService.getUnreadMessages(ChatFixture.USER_ID, ChatFixture.ROOM_ID, null, 20),
                    ErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }

        @Test
        @DisplayName("cursor=null, lastReadMessageId 있으면 findByRoomIdAndIdGreaterThan... 호출")
        void cursor_null_lastReadMessageId_있으면_GreaterThan_호출() {
            ChatRoom room = ChatFixture.chatRoom(ChatFixture.ROOM_ID);
            ChatParticipant participant = ChatFixture.chatParticipant(room, ChatFixture.USER_ID);
            participant.updateLastReadMessageId(ChatFixture.VALID_OID);
            given(chatParticipantRepository.findByRoomIdAndUserId(ChatFixture.ROOM_ID, ChatFixture.USER_ID))
                    .willReturn(Optional.of(participant));
            given(chatMessageRepository.findByRoomIdAndIdGreaterThanAndCreatedAtGreaterThanEqual(
                            eq(ChatFixture.ROOM_ID), any(ObjectId.class), any(Instant.class), any()))
                    .willReturn(List.of());

            chatRoomService.getUnreadMessages(ChatFixture.USER_ID, ChatFixture.ROOM_ID, null, 20);

            then(chatMessageRepository)
                    .should()
                    .findByRoomIdAndIdGreaterThanAndCreatedAtGreaterThanEqual(
                            eq(ChatFixture.ROOM_ID), any(ObjectId.class), any(Instant.class), any());
        }

        @Test
        @DisplayName("cursor=null, lastReadMessageId 없으면 findByRoomIdAndCreatedAtGreaterThanEqual 호출")
        void cursor_null_lastReadMessageId_없으면_CreatedAt_호출() {
            ChatRoom room = ChatFixture.chatRoom(ChatFixture.ROOM_ID);
            ChatParticipant participant = ChatFixture.chatParticipant(room, ChatFixture.USER_ID);
            given(chatParticipantRepository.findByRoomIdAndUserId(ChatFixture.ROOM_ID, ChatFixture.USER_ID))
                    .willReturn(Optional.of(participant));
            given(chatMessageRepository.findByRoomIdAndCreatedAtGreaterThanEqual(
                            eq(ChatFixture.ROOM_ID), any(Instant.class), any()))
                    .willReturn(List.of());

            chatRoomService.getUnreadMessages(ChatFixture.USER_ID, ChatFixture.ROOM_ID, null, 20);

            then(chatMessageRepository)
                    .should()
                    .findByRoomIdAndCreatedAtGreaterThanEqual(eq(ChatFixture.ROOM_ID), any(Instant.class), any());
        }

        @Test
        @DisplayName("유효한 cursor 있으면 findByRoomIdAndIdGreaterThan... 호출 (페이지네이션)")
        void 유효한_cursor_GreaterThan_호출() {
            ChatRoom room = ChatFixture.chatRoom(ChatFixture.ROOM_ID);
            ChatParticipant participant = ChatFixture.chatParticipant(room, ChatFixture.USER_ID);
            given(chatParticipantRepository.findByRoomIdAndUserId(ChatFixture.ROOM_ID, ChatFixture.USER_ID))
                    .willReturn(Optional.of(participant));
            given(chatMessageRepository.findByRoomIdAndIdGreaterThanAndCreatedAtGreaterThanEqual(
                            eq(ChatFixture.ROOM_ID), any(ObjectId.class), any(Instant.class), any()))
                    .willReturn(List.of());

            chatRoomService.getUnreadMessages(ChatFixture.USER_ID, ChatFixture.ROOM_ID, ChatFixture.VALID_OID, 20);

            then(chatMessageRepository)
                    .should()
                    .findByRoomIdAndIdGreaterThanAndCreatedAtGreaterThanEqual(
                            eq(ChatFixture.ROOM_ID), any(ObjectId.class), any(Instant.class), any());
        }

        @Test
        @DisplayName("유효하지 않은 cursor면 lastReadMessageId 기준으로 폴백한다")
        void 유효하지_않은_cursor_lastReadMessageId_폴백() {
            ChatRoom room = ChatFixture.chatRoom(ChatFixture.ROOM_ID);
            ChatParticipant participant = ChatFixture.chatParticipant(room, ChatFixture.USER_ID);
            participant.updateLastReadMessageId(ChatFixture.VALID_OID);
            given(chatParticipantRepository.findByRoomIdAndUserId(ChatFixture.ROOM_ID, ChatFixture.USER_ID))
                    .willReturn(Optional.of(participant));
            given(chatMessageRepository.findByRoomIdAndIdGreaterThanAndCreatedAtGreaterThanEqual(
                            eq(ChatFixture.ROOM_ID), any(ObjectId.class), any(Instant.class), any()))
                    .willReturn(List.of());

            chatRoomService.getUnreadMessages(ChatFixture.USER_ID, ChatFixture.ROOM_ID, ChatFixture.INVALID_OID, 20);

            then(chatMessageRepository)
                    .should()
                    .findByRoomIdAndIdGreaterThanAndCreatedAtGreaterThanEqual(
                            eq(ChatFixture.ROOM_ID), any(ObjectId.class), any(Instant.class), any());
        }

        @Test
        @DisplayName("메시지가 있으면 applyReadEffect — ChatReadEvent 발행")
        void 메시지_있으면_읽음_이벤트_발행() {
            ChatRoom room = ChatFixture.chatRoom(ChatFixture.ROOM_ID);
            ChatParticipant participant = ChatFixture.chatParticipant(room, ChatFixture.USER_ID);
            ChatMessage message = ChatFixture.chatMessage(ChatFixture.ROOM_ID, ChatFixture.USER_ID);
            given(chatParticipantRepository.findByRoomIdAndUserId(ChatFixture.ROOM_ID, ChatFixture.USER_ID))
                    .willReturn(Optional.of(participant));
            given(chatMessageRepository.findByRoomIdAndCreatedAtGreaterThanEqual(
                            eq(ChatFixture.ROOM_ID), any(Instant.class), any()))
                    .willReturn(List.of(message));

            chatRoomService.getUnreadMessages(ChatFixture.USER_ID, ChatFixture.ROOM_ID, null, 20);

            then(eventPublisher).should().publishEvent(any(ChatReadEvent.class));
        }

        @Test
        @DisplayName("첫 진입(cursor=null)이고 메시지 없으면 unread reset 이벤트 발행")
        void 첫_진입_메시지_없어도_unread_reset_이벤트_발행() {
            ChatRoom room = ChatFixture.chatRoom(ChatFixture.ROOM_ID);
            ChatParticipant participant = ChatFixture.chatParticipant(room, ChatFixture.USER_ID);
            given(chatParticipantRepository.findByRoomIdAndUserId(ChatFixture.ROOM_ID, ChatFixture.USER_ID))
                    .willReturn(Optional.of(participant));
            given(chatMessageRepository.findByRoomIdAndCreatedAtGreaterThanEqual(
                            eq(ChatFixture.ROOM_ID), any(Instant.class), any()))
                    .willReturn(List.of());

            chatRoomService.getUnreadMessages(ChatFixture.USER_ID, ChatFixture.ROOM_ID, null, 20);

            then(eventPublisher).should().publishEvent(any(ChatReadEvent.class));
        }

        @Test
        @DisplayName("페이징 호출(cursor!=null)이고 메시지 없으면 이벤트 미발행")
        void 페이징_호출_메시지_없으면_이벤트_미발행() {
            ChatRoom room = ChatFixture.chatRoom(ChatFixture.ROOM_ID);
            ChatParticipant participant = ChatFixture.chatParticipant(room, ChatFixture.USER_ID);
            given(chatParticipantRepository.findByRoomIdAndUserId(ChatFixture.ROOM_ID, ChatFixture.USER_ID))
                    .willReturn(Optional.of(participant));
            given(chatMessageRepository.findByRoomIdAndIdGreaterThanAndCreatedAtGreaterThanEqual(
                            eq(ChatFixture.ROOM_ID), any(ObjectId.class), any(Instant.class), any()))
                    .willReturn(List.of());

            chatRoomService.getUnreadMessages(ChatFixture.USER_ID, ChatFixture.ROOM_ID, ChatFixture.VALID_OID, 20);

            verifyNoInteractions(eventPublisher);
        }

        @Test
        @DisplayName("limit+1 반환 시 hasNextPage=true")
        void limit_plus1_반환시_hasNextPage_true() {
            ChatRoom room = ChatFixture.chatRoom(ChatFixture.ROOM_ID);
            ChatParticipant participant = ChatFixture.chatParticipant(room, ChatFixture.USER_ID);
            // limit=2, 3개 반환 → hasNextPage=true
            ChatMessage m1 = ChatFixture.chatMessage(ChatFixture.ROOM_ID, ChatFixture.USER_ID);
            ChatMessage m2 = ChatFixture.chatMessage(ChatFixture.ROOM_ID, ChatFixture.USER_ID);
            ChatMessage m3 = ChatFixture.chatMessage(ChatFixture.ROOM_ID, ChatFixture.USER_ID);
            given(chatParticipantRepository.findByRoomIdAndUserId(ChatFixture.ROOM_ID, ChatFixture.USER_ID))
                    .willReturn(Optional.of(participant));
            given(chatMessageRepository.findByRoomIdAndCreatedAtGreaterThanEqual(
                            eq(ChatFixture.ROOM_ID), any(Instant.class), any()))
                    .willReturn(List.of(m1, m2, m3));

            var response = chatRoomService.getUnreadMessages(ChatFixture.USER_ID, ChatFixture.ROOM_ID, null, 2);

            assertThat(response.hasNextPage()).isTrue();
        }

        @Test
        @DisplayName("nextCursor는 마지막 메시지 ID (ASC 정렬 기준 가장 최신)")
        void nextCursor_마지막_메시지_ID() {
            ChatRoom room = ChatFixture.chatRoom(ChatFixture.ROOM_ID);
            ChatParticipant participant = ChatFixture.chatParticipant(room, ChatFixture.USER_ID);
            ObjectId id1 = new ObjectId(ChatFixture.OLDER_OID);
            ObjectId id2 = new ObjectId(ChatFixture.VALID_OID);
            ChatMessage m1 = ChatMessage.builder()
                    .id(id1)
                    .roomId(ChatFixture.ROOM_ID)
                    .senderId(ChatFixture.USER_ID)
                    .type("TEXT")
                    .content("첫 번째")
                    .createdAt(Instant.now())
                    .build();
            ChatMessage m2 = ChatMessage.builder()
                    .id(id2)
                    .roomId(ChatFixture.ROOM_ID)
                    .senderId(ChatFixture.USER_ID)
                    .type("TEXT")
                    .content("두 번째 (최신)")
                    .createdAt(Instant.now())
                    .build();
            ChatMessage extra = ChatFixture.chatMessage(ChatFixture.ROOM_ID, ChatFixture.USER_ID);
            given(chatParticipantRepository.findByRoomIdAndUserId(ChatFixture.ROOM_ID, ChatFixture.USER_ID))
                    .willReturn(Optional.of(participant));
            // limit=2, 3개 반환 → hasNextPage=true, nextCursor = m2.id (마지막 페이지 항목)
            given(chatMessageRepository.findByRoomIdAndCreatedAtGreaterThanEqual(
                            eq(ChatFixture.ROOM_ID), any(Instant.class), any()))
                    .willReturn(List.of(m1, m2, extra));

            var response = chatRoomService.getUnreadMessages(ChatFixture.USER_ID, ChatFixture.ROOM_ID, null, 2);

            assertThat(response.nextCursor()).isEqualTo(id2.toHexString());
        }
    }

    // ======================== getUnreadStatus ========================

    @Nested
    @DisplayName("getUnreadStatus")
    class GetUnreadStatus {

        @Test
        @DisplayName("참여한 방이 없으면 hasUnread=false, totalUnreadCount=0")
        void 참여한_방_없음() {
            given(chatParticipantRepository.findByUserId(ChatFixture.USER_ID)).willReturn(List.of());

            UnreadStatusResponse response = chatRoomService.getUnreadStatus(ChatFixture.USER_ID);

            assertThat(response.hasUnread()).isFalse();
            assertThat(response.totalUnreadCount()).isZero();
        }

        @Test
        @DisplayName("방이 있지만 모든 unread가 0이면 hasUnread=false")
        void 방_있으나_unread_모두_0() {
            ChatRoom room = ChatFixture.chatRoom(ChatFixture.ROOM_ID);
            ChatParticipant participant = ChatFixture.chatParticipant(room, ChatFixture.USER_ID);
            given(chatParticipantRepository.findByUserId(ChatFixture.USER_ID)).willReturn(List.of(participant));
            given(chatRedisRepository.getUnread(ChatFixture.USER_ID, ChatFixture.ROOM_ID))
                    .willReturn(0L);

            UnreadStatusResponse response = chatRoomService.getUnreadStatus(ChatFixture.USER_ID);

            assertThat(response.hasUnread()).isFalse();
            assertThat(response.totalUnreadCount()).isZero();
        }

        @Test
        @DisplayName("여러 방의 unread를 합산하여 반환한다")
        void 여러_방_unread_합산() {
            long roomId2 = 20L;
            ChatRoom room1 = ChatFixture.chatRoom(ChatFixture.ROOM_ID);
            ChatRoom room2 = ChatFixture.chatRoom(roomId2);
            ChatParticipant p1 = ChatFixture.chatParticipant(room1, ChatFixture.USER_ID);
            ChatParticipant p2 = ChatFixture.chatParticipant(room2, ChatFixture.USER_ID);
            given(chatParticipantRepository.findByUserId(ChatFixture.USER_ID)).willReturn(List.of(p1, p2));
            given(chatRedisRepository.getUnread(ChatFixture.USER_ID, ChatFixture.ROOM_ID))
                    .willReturn(3L);
            given(chatRedisRepository.getUnread(ChatFixture.USER_ID, roomId2)).willReturn(2L);

            UnreadStatusResponse response = chatRoomService.getUnreadStatus(ChatFixture.USER_ID);

            assertThat(response.hasUnread()).isTrue();
            assertThat(response.totalUnreadCount()).isEqualTo(5L);
        }
    }

    // ======================== 헬퍼 ========================

    private void assertCustomException(ThrowingCallable callable, ErrorCode expectedCode) {
        assertThatThrownBy(callable)
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(expectedCode);
    }
}
