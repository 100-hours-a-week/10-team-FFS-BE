package com.example.kloset_lab.chat.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.kloset_lab.chat.entity.ChatParticipant;
import com.example.kloset_lab.chat.entity.ChatRoom;
import com.example.kloset_lab.global.base.JpaRepositoryTest;
import com.example.kloset_lab.user.entity.Provider;
import com.example.kloset_lab.user.entity.User;
import com.example.kloset_lab.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("ChatRoomRepository 슬라이스 테스트")
class ChatRoomRepositoryTest extends JpaRepositoryTest {

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ChatParticipantRepository chatParticipantRepository;

    @Autowired
    private UserRepository userRepository;

    // ─── 헬퍼 ───────────────────────────────────────────────────────────────

    /** 채팅방 생성 및 저장 */
    private ChatRoom saveRoom() {
        return chatRoomRepository.save(ChatRoom.create());
    }

    /** providerId는 중복 불가이므로 호출마다 고유값을 넘긴다 */
    private User saveUser(String providerId) {
        return userRepository.save(
                User.builder().provider(Provider.KAKAO).providerId(providerId).build());
    }

    private ChatParticipant addParticipant(ChatRoom room, User user) {
        return chatParticipantRepository.save(
                ChatParticipant.builder().room(room).user(user).build());
    }

    /** 채팅방 소프트 삭제 */
    private ChatRoom softDelete(ChatRoom room) {
        room.softDelete();
        return chatRoomRepository.save(room);
    }

    // ─── 테스트 ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("findExistingRoomBetweenUsers — 두 유저 간 채팅방 조회")
    class FindExistingRoomBetweenUsers {

        @Test
        @DisplayName("두 유저가 참여한 채팅방을 반환한다")
        void 두_유저_채팅방_반환() {
            ChatRoom room = saveRoom();
            User user1 = saveUser("user-a1");
            User user2 = saveUser("user-a2");
            addParticipant(room, user1);
            addParticipant(room, user2);

            Optional<ChatRoom> result = chatRoomRepository.findExistingRoomBetweenUsers(user1.getId(), user2.getId());

            assertThat(result).isPresent().get().extracting(ChatRoom::getId).isEqualTo(room.getId());
        }

        @Test
        @DisplayName("두 유저 조회 순서가 반대여도 동일한 채팅방을 반환한다")
        void 조회_순서_무관() {
            ChatRoom room = saveRoom();
            User user1 = saveUser("user-b1");
            User user2 = saveUser("user-b2");
            addParticipant(room, user1);
            addParticipant(room, user2);

            Optional<ChatRoom> result = chatRoomRepository.findExistingRoomBetweenUsers(user2.getId(), user1.getId());

            assertThat(result).isPresent().get().extracting(ChatRoom::getId).isEqualTo(room.getId());
        }

        @Test
        @DisplayName("채팅방이 소프트 삭제된 경우 결과에 포함되지 않는다")
        void 소프트_삭제_채팅방_제외() {
            ChatRoom room = saveRoom();
            User user1 = saveUser("user-c1");
            User user2 = saveUser("user-c2");
            addParticipant(room, user1);
            addParticipant(room, user2);
            softDelete(room);

            Optional<ChatRoom> result = chatRoomRepository.findExistingRoomBetweenUsers(user1.getId(), user2.getId());

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("두 유저 외 제3자가 참여한 채팅방은 반환하지 않는다")
        void 제3자_포함_채팅방_제외() {
            ChatRoom room = saveRoom();
            User user1 = saveUser("user-d1");
            User user2 = saveUser("user-d2");
            User user3 = saveUser("user-d3");
            addParticipant(room, user1);
            addParticipant(room, user2);
            addParticipant(room, user3);

            Optional<ChatRoom> result = chatRoomRepository.findExistingRoomBetweenUsers(user1.getId(), user2.getId());

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("상대방만 참여한 다른 채팅방은 결과에 포함되지 않는다")
        void 상대방만_있는_다른_방_미포함() {
            User user1 = saveUser("user-e1");
            User user2 = saveUser("user-e2");
            User user3 = saveUser("user-e3");

            ChatRoom roomA = saveRoom();
            addParticipant(roomA, user1);
            addParticipant(roomA, user2);

            ChatRoom roomB = saveRoom();
            addParticipant(roomB, user2);
            addParticipant(roomB, user3);

            Optional<ChatRoom> result = chatRoomRepository.findExistingRoomBetweenUsers(user1.getId(), user2.getId());

            assertThat(result).isPresent().get().extracting(ChatRoom::getId).isEqualTo(roomA.getId());
        }

        @Test
        @DisplayName("공통 채팅방이 없으면 empty를 반환한다")
        void 공통_채팅방_없으면_empty() {
            User user1 = saveUser("user-f1");
            User user2 = saveUser("user-f2");

            ChatRoom room1 = saveRoom();
            addParticipant(room1, user1);

            ChatRoom room2 = saveRoom();
            addParticipant(room2, user2);

            Optional<ChatRoom> result = chatRoomRepository.findExistingRoomBetweenUsers(user1.getId(), user2.getId());

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("userId가 이전에 나간 채팅방이어도 상대방이 활성이면 반환한다 (DM 재진입용)")
        void userId가_나간_후에도_상대방_활성이면_반환() {
            ChatRoom room = saveRoom();
            User user1 = saveUser("user-g1");
            User user2 = saveUser("user-g2");
            ChatParticipant cp1 = addParticipant(room, user1);
            addParticipant(room, user2);

            cp1.leave();
            chatParticipantRepository.save(cp1);

            Optional<ChatRoom> result = chatRoomRepository.findExistingRoomBetweenUsers(user1.getId(), user2.getId());

            assertThat(result).isPresent().get().extracting(ChatRoom::getId).isEqualTo(room.getId());
        }

        @Test
        @DisplayName("opponentUserId가 나간 경우 empty를 반환한다")
        void opponentUserId가_나갔으면_empty() {
            ChatRoom room = saveRoom();
            User user1 = saveUser("user-h1");
            User user2 = saveUser("user-h2");
            addParticipant(room, user1);
            ChatParticipant cp2 = addParticipant(room, user2);

            cp2.leave();
            chatParticipantRepository.save(cp2);

            Optional<ChatRoom> result = chatRoomRepository.findExistingRoomBetweenUsers(user1.getId(), user2.getId());

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAllByDeletedAtIsNotNull — 소프트 삭제된 채팅방 조회")
    class FindAllByDeletedAtIsNotNull {

        @Test
        @DisplayName("소프트 삭제된 채팅방만 반환하고 활성 채팅방은 제외한다")
        void 소프트_삭제된_채팅방만_반환() {
            saveRoom();
            ChatRoom deleted = softDelete(saveRoom());

            var result = chatRoomRepository.findAllByDeletedAtIsNotNull();

            assertThat(result).hasSize(1).extracting(ChatRoom::getId).containsExactly(deleted.getId());
        }
    }

    @Nested
    @DisplayName("findAllByDeletedAtIsNull — 활성 채팅방 조회")
    class FindAllByDeletedAtIsNull {

        @Test
        @DisplayName("활성 채팅방만 반환하고 소프트 삭제된 채팅방은 제외한다")
        void 활성_채팅방만_반환() {
            ChatRoom active = saveRoom();
            softDelete(saveRoom());

            var result = chatRoomRepository.findAllByDeletedAtIsNull();

            assertThat(result).hasSize(1).extracting(ChatRoom::getId).containsExactly(active.getId());
        }
    }
}
