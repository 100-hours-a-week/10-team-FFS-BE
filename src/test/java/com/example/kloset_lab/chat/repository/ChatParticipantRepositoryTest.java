package com.example.kloset_lab.chat.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.kloset_lab.chat.entity.ChatParticipant;
import com.example.kloset_lab.chat.entity.ChatRoom;
import com.example.kloset_lab.global.base.JpaRepositoryTest;
import com.example.kloset_lab.user.entity.Provider;
import com.example.kloset_lab.user.entity.User;
import com.example.kloset_lab.user.repository.UserRepository;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("ChatParticipantRepository 슬라이스 테스트")
class ChatParticipantRepositoryTest extends JpaRepositoryTest {

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ChatParticipantRepository chatParticipantRepository;

    @Autowired
    private UserRepository userRepository;

    /** providerId 충돌 방지용 카운터 */
    private final AtomicInteger seq = new AtomicInteger();

    // ─── 헬퍼 ───────────────────────────────────────────────────────────────

    private ChatRoom saveRoom() {
        return chatRoomRepository.save(ChatRoom.create());
    }

    private User saveUser() {
        return userRepository.save(User.builder()
                .provider(Provider.KAKAO)
                .providerId("u" + seq.incrementAndGet())
                .build());
    }

    private ChatParticipant addParticipant(ChatRoom room, User user) {
        return chatParticipantRepository.save(
                ChatParticipant.builder().room(room).user(user).build());
    }

    // ─── 테스트 ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("findByRoomIdAndUserId — 활성 참여자 조회 (leftAt IS NULL)")
    class FindByRoomIdAndUserId {

        @Test
        @DisplayName("활성 참여자를 반환한다")
        void 활성_참여자_반환() {
            ChatRoom room = saveRoom();
            User user = saveUser();
            ChatParticipant participant = addParticipant(room, user);

            var result = chatParticipantRepository.findByRoomIdAndUserId(room.getId(), user.getId());

            assertThat(result)
                    .isPresent()
                    .get()
                    .extracting(ChatParticipant::getId)
                    .isEqualTo(participant.getId());
        }

        @Test
        @DisplayName("나간 참여자는 empty를 반환한다")
        void 나간_참여자_empty() {
            ChatRoom room = saveRoom();
            User user = saveUser();
            ChatParticipant cp = addParticipant(room, user);

            cp.leave();
            chatParticipantRepository.save(cp);

            assertThat(chatParticipantRepository.findByRoomIdAndUserId(room.getId(), user.getId()))
                    .isEmpty();
        }

        @Test
        @DisplayName("채팅방에 참여하지 않은 사용자는 empty를 반환한다")
        void 미참여_사용자_empty() {
            ChatRoom room = saveRoom();
            User user1 = saveUser();
            User user2 = saveUser();
            addParticipant(room, user1);

            assertThat(chatParticipantRepository.findByRoomIdAndUserId(room.getId(), user2.getId()))
                    .isEmpty();
        }

        @Test
        @DisplayName("다른 채팅방의 동일 사용자는 반환하지 않는다")
        void 다른_방_동일_사용자_미포함() {
            ChatRoom roomA = saveRoom();
            ChatRoom roomB = saveRoom();
            User user = saveUser();
            addParticipant(roomB, user);

            assertThat(chatParticipantRepository.findByRoomIdAndUserId(roomA.getId(), user.getId()))
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("findHistoryByRoomIdAndUserId — 히스토리 참여자 조회 (나간 사용자 포함)")
    class FindHistoryByRoomIdAndUserId {

        @Test
        @DisplayName("나간 참여자도 히스토리로 조회된다")
        void 나간_참여자_히스토리_조회() {
            ChatRoom room = saveRoom();
            User user = saveUser();
            ChatParticipant cp = addParticipant(room, user);

            cp.leave();
            chatParticipantRepository.save(cp);

            assertThat(chatParticipantRepository.findHistoryByRoomIdAndUserId(room.getId(), user.getId()))
                    .isPresent()
                    .get()
                    .satisfies(p -> assertThat(p.getLeftAt()).isNotNull());
        }

        @Test
        @DisplayName("참여한 적 없는 사용자는 empty를 반환한다")
        void 미참여_사용자_empty() {
            ChatRoom room = saveRoom();
            User user = saveUser();

            assertThat(chatParticipantRepository.findHistoryByRoomIdAndUserId(room.getId(), user.getId()))
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("countByRoomId — 활성 참여자 수 조회")
    class CountByRoomId {

        @Test
        @DisplayName("활성 참여자 수를 반환한다")
        void 참여자_수_반환() {
            ChatRoom room = saveRoom();
            addParticipant(room, saveUser());
            addParticipant(room, saveUser());

            assertThat(chatParticipantRepository.countByRoomId(room.getId())).isEqualTo(2);
        }

        @Test
        @DisplayName("나간 참여자는 카운트에 포함되지 않는다")
        void 나간_참여자_미포함() {
            ChatRoom room = saveRoom();
            ChatParticipant cp = addParticipant(room, saveUser());
            addParticipant(room, saveUser());

            cp.leave();
            chatParticipantRepository.save(cp);

            assertThat(chatParticipantRepository.countByRoomId(room.getId())).isEqualTo(1);
        }

        @Test
        @DisplayName("다른 채팅방 참여자는 카운트에 포함되지 않는다")
        void 다른_방_참여자_미포함() {
            ChatRoom roomA = saveRoom();
            ChatRoom roomB = saveRoom();
            addParticipant(roomA, saveUser());
            addParticipant(roomB, saveUser());

            assertThat(chatParticipantRepository.countByRoomId(roomA.getId())).isEqualTo(1);
        }

        @Test
        @DisplayName("참여자가 없는 채팅방은 0을 반환한다")
        void 참여자_없으면_0() {
            ChatRoom room = saveRoom();

            assertThat(chatParticipantRepository.countByRoomId(room.getId())).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("leave / reenter — soft delete 및 재진입")
    class Leave {

        @Test
        @DisplayName("나간 후 활성 조회로는 조회되지 않고 다른 참여자는 유지된다")
        void 나간_후_활성_조회_불가() {
            ChatRoom room = saveRoom();
            User user1 = saveUser();
            User user2 = saveUser();
            ChatParticipant cp1 = addParticipant(room, user1);
            addParticipant(room, user2);

            cp1.leave();
            chatParticipantRepository.save(cp1);

            assertThat(chatParticipantRepository.findByRoomIdAndUserId(room.getId(), user1.getId()))
                    .isEmpty();
            assertThat(chatParticipantRepository.findByRoomIdAndUserId(room.getId(), user2.getId()))
                    .isPresent();
        }

        @Test
        @DisplayName("다른 채팅방의 동일 사용자는 나가기의 영향을 받지 않는다")
        void 다른_방_동일_사용자_유지() {
            ChatRoom roomA = saveRoom();
            ChatRoom roomB = saveRoom();
            User user = saveUser();
            ChatParticipant cpA = addParticipant(roomA, user);
            addParticipant(roomB, user);

            cpA.leave();
            chatParticipantRepository.save(cpA);

            assertThat(chatParticipantRepository.findByRoomIdAndUserId(roomB.getId(), user.getId()))
                    .isPresent();
        }

        @Test
        @DisplayName("재진입 시 leftAt이 null로 초기화되고 활성 조회가 가능해진다")
        void 재진입_시_활성화() {
            ChatRoom room = saveRoom();
            User user = saveUser();
            ChatParticipant cp = addParticipant(room, user);

            cp.leave();
            chatParticipantRepository.save(cp);
            cp.reenter();
            chatParticipantRepository.save(cp);

            assertThat(chatParticipantRepository.findByRoomIdAndUserId(room.getId(), user.getId()))
                    .isPresent()
                    .get()
                    .satisfies(p -> assertThat(p.getLeftAt()).isNull());
        }
    }

    @Nested
    @DisplayName("findByRoomId — 전체 참여자 조회 (나간 사용자 포함)")
    class FindByRoomId {

        @Test
        @DisplayName("모든 참여자를 반환한다")
        void 전체_참여자_반환() {
            ChatRoom room = saveRoom();
            User user1 = saveUser();
            User user2 = saveUser();
            addParticipant(room, user1);
            addParticipant(room, user2);

            assertThat(chatParticipantRepository.findByRoomId(room.getId()))
                    .hasSize(2)
                    .extracting(p -> p.getUser().getId())
                    .containsExactlyInAnyOrder(user1.getId(), user2.getId());
        }

        @Test
        @DisplayName("나간 참여자도 전체 조회에 포함된다")
        void 나간_참여자_포함() {
            ChatRoom room = saveRoom();
            User user = saveUser();
            ChatParticipant cp = addParticipant(room, user);

            cp.leave();
            chatParticipantRepository.save(cp);

            assertThat(chatParticipantRepository.findByRoomId(room.getId())).hasSize(1);
        }

        @Test
        @DisplayName("참여자가 없는 채팅방은 빈 목록을 반환한다")
        void 빈_채팅방_빈_목록() {
            ChatRoom room = saveRoom();

            assertThat(chatParticipantRepository.findByRoomId(room.getId())).isEmpty();
        }
    }

    @Nested
    @DisplayName("findActiveByRoomId — 활성 참여자 조회 (나간 사용자 제외, 브로드캐스트용)")
    class FindActiveByRoomId {

        @Test
        @DisplayName("나간 참여자는 활성 목록에서 제외된다")
        void 나간_참여자_제외() {
            ChatRoom room = saveRoom();
            User user1 = saveUser();
            User user2 = saveUser();
            ChatParticipant cp1 = addParticipant(room, user1);
            addParticipant(room, user2);

            cp1.leave();
            chatParticipantRepository.save(cp1);

            assertThat(chatParticipantRepository.findActiveByRoomId(room.getId()))
                    .hasSize(1)
                    .extracting(p -> p.getUser().getId())
                    .containsExactly(user2.getId());
        }
    }

    @Nested
    @DisplayName("findByUserId — 사용자가 활성 참여 중인 채팅방 조회")
    class FindByUserId {

        @Test
        @DisplayName("활성 채팅방 참여자 레코드를 반환한다")
        void 사용자_참여_방_목록_반환() {
            ChatRoom roomA = saveRoom();
            ChatRoom roomB = saveRoom();
            ChatRoom roomC = saveRoom();
            User user1 = saveUser();
            User user2 = saveUser();
            addParticipant(roomA, user1);
            addParticipant(roomB, user1);
            addParticipant(roomC, user2);

            List<ChatParticipant> result = chatParticipantRepository.findByUserId(user1.getId());

            assertThat(result)
                    .hasSize(2)
                    .extracting(p -> p.getRoom().getId())
                    .containsExactlyInAnyOrder(roomA.getId(), roomB.getId());
        }

        @Test
        @DisplayName("나간 채팅방은 조회 결과에 포함되지 않는다")
        void 나간_방_미포함() {
            ChatRoom roomA = saveRoom();
            ChatRoom roomB = saveRoom();
            User user = saveUser();
            ChatParticipant cpA = addParticipant(roomA, user);
            addParticipant(roomB, user);

            cpA.leave();
            chatParticipantRepository.save(cpA);

            assertThat(chatParticipantRepository.findByUserId(user.getId()))
                    .hasSize(1)
                    .extracting(p -> p.getRoom().getId())
                    .containsExactly(roomB.getId());
        }

        @Test
        @DisplayName("참여한 채팅방이 없으면 빈 목록을 반환한다")
        void 참여_방_없으면_빈_목록() {
            User user = saveUser();

            assertThat(chatParticipantRepository.findByUserId(user.getId())).isEmpty();
        }
    }
}
