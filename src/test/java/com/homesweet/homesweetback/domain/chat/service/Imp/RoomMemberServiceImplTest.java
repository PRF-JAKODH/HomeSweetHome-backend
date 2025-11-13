package com.homesweet.homesweetback.domain.chat.service.Imp;

import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.repository.UserRepository;
import com.homesweet.homesweetback.domain.chat.entity.ChatRoom;
import com.homesweet.homesweetback.domain.chat.entity.RoomMember;
import com.homesweet.homesweetback.domain.chat.entity.enums.ChatUserRole;
import com.homesweet.homesweetback.domain.chat.repository.ChatRoomRepository;
import com.homesweet.homesweetback.domain.chat.repository.RoomMemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 *
 * @author hygg0408e@gmail.com
 * @date 25. 11. 12.
 */
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
@DisplayName("[Service] 채팅방 멤버 서비스 단위 테스트")
class RoomMemberServiceImplTest {

    @InjectMocks
    private RoomMemberServiceImpl service;

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoomMemberRepository roomMemberRepository;
    @Mock
    private ChatRoomRepository chatRoomRepository;


    @Nested
    @DisplayName("[성공] 개인 채팅방 테스트")
    class createIndividualRoom {

        @Nested
        @DisplayName("개인 채팅방 멤버 등록")
        class RegisterIndividualMember {

            @Test
            @DisplayName("개인 채팅방의 참여자를 저장한다. ")
                // 클래스로 구분하라아아라라
            void shouldRegisterTwoMembers() {
                // given
                ChatRoom room = ChatRoom.builder().id(1L).build();
                User me = User.builder().id(1L).build();
                User target = User.builder().id(2L).build();

                given(userRepository.findById(1L)).willReturn(Optional.of(me));
                given(userRepository.findById(2L)).willReturn(Optional.of(target));

                ArgumentCaptor<List<RoomMember>> captor = ArgumentCaptor.forClass(List.class);

                // when
                service.registerIndividualMember(room, 1L, 2L);

                // then ?
                verify(roomMemberRepository).saveAll(captor.capture());

                List<RoomMember> members = captor.getValue();
                assertThat(members).hasSize(2);
                assertThat(members.get(0).getRole()).isEqualTo(ChatUserRole.OWNER);
                assertThat(members.get(1).getRole()).isEqualTo(ChatUserRole.MEMBER);
            }
        }

        @Nested
        @DisplayName(" 개인 채팅방 userId로 pairKey를 생성한다. ")
        class buildPairKey {

            @Test
            @DisplayName("작은 ID가 먼저 오면 'low:high' 형식으로 반환")
            void shouldReturnLowHighFormat() {
                // when
                String result = service.buildPairKey(1L, 5L);

                // then
                assertThat(result).isEqualTo("1:5");
            }

            @Test
            @DisplayName("큰 ID가 먼저 와도 자동 정렬하여 'low:high' 반환")
            void shouldSortToLowHighFormat() {
                // when
                String result = service.buildPairKey(10L, 3L);

                // then
                assertThat(result).isEqualTo("3:10");
            }

            @Test
            @DisplayName("같은 ID는 'id:id' 형식으로 반환")
            void shouldHandleSameId() {
                // when
                String result = service.buildPairKey(7L, 7L);

                // then
                assertThat(result).isEqualTo("7:7");
            }
        }
    }
}