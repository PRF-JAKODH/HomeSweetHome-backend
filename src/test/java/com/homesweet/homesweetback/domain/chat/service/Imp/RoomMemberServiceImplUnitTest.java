package com.homesweet.homesweetback.domain.chat.service.Imp;

import com.homesweet.homesweetback.common.exception.BusinessException;
import com.homesweet.homesweetback.common.exception.ErrorCode;
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
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 *
 * @author hygg0408e@gmail.com
 * @date 25. 11. 12.
 */
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
@DisplayName("[Service] 채팅방 멤버 서비스 단위 테스트")
class RoomMemberServiceImplUnitTest {

    @InjectMocks
    private RoomMemberServiceImpl service;

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoomMemberRepository roomMemberRepository;
    @Mock
    private ChatRoomRepository chatRoomRepository;


    @Nested
    @DisplayName("개인 채팅방 테스트")
    class createIndividualRoom {

        @Nested
        @DisplayName("[단위] 개인 채팅방 멤버 등록")
        class RegisterIndividualMember {

            @Test
            @DisplayName("[성공] 개인 채팅방의 참여자를 저장한다.")
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
        @DisplayName(" 개인 채팅방 userId로 pairKey를 생성 ")
        class buildPairKey {

            @Test
            @DisplayName("[성공] 작은 ID가 먼저 오면 'low:high' 형식으로 반환")
            void shouldReturnLowHighFormat() {
                // when
                String result = service.buildPairKey(1L, 5L);

                // then
                assertThat(result).isEqualTo("1:5");
            }

            @Test
            @DisplayName("[성공] 큰 ID가 먼저 와도 자동 정렬하여 'low:high' 반환")
            void shouldSortToLowHighFormat() {
                // when
                String result = service.buildPairKey(10L, 3L);

                // then
                assertThat(result).isEqualTo("3:10");
            }

            @Test
            @DisplayName("[성공] 같은 ID는 'id:id' 형식으로 반환")
            void shouldHandleSameId() {
                // when
                String result = service.buildPairKey(7L, 7L);

                // then
                assertThat(result).isEqualTo("7:7");
            }
        }
    }

    @Nested
    @DisplayName("그룹채팅방 테스트")
    class RegisterGroupMember {

        @Nested
        @DisplayName("그룹채팅방 멤버 등록 및 재입장")
        class RegisterMember {

            @Test
            @DisplayName("[성공] 신규 멤버를 MEMBER 역할로 등록한다")
            void shouldRegisterNewMember() {
                // given
                ChatRoom chatRoom = ChatRoom.builder().id(100L).build();
                User user = User.builder().id(1L).name("철수").build();

                given(roomMemberRepository.findByRoomIdAndUserId(100L, 1L))
                        .willReturn(Optional.empty());  // 멤버 없음
                given(userRepository.findById(1L))
                        .willReturn(Optional.of(user));

                ArgumentCaptor<RoomMember> captor = ArgumentCaptor.forClass(RoomMember.class);

                // when
                service.registerGroupMember(chatRoom, 1L);

                // then
                verify(roomMemberRepository).save(captor.capture());

                RoomMember savedMember = captor.getValue();
                assertThat(savedMember.getUser()).isEqualTo(user);
                assertThat(savedMember.getRoom()).isEqualTo(chatRoom);
                assertThat(savedMember.getRole()).isEqualTo(ChatUserRole.MEMBER);
                assertThat(savedMember.isExit()).isFalse();
            }

            @Test
            @DisplayName("[성공] 퇴장한 멤버를 재입장 처리한다")
            void shouldRejoinExitedMember() {
                // given
                ChatRoom chatRoom = ChatRoom.builder().id(100L).build();
                User user = User.builder().id(1L).name("철수").build();

                RoomMember exitedMember = RoomMember.createMember(chatRoom, user, ChatUserRole.MEMBER);
                exitedMember.exit();

                given(roomMemberRepository.findByRoomIdAndUserId(100L, 1L))
                        .willReturn(Optional.of(exitedMember));

                // when
                service.registerGroupMember(chatRoom, 1L);

                // then
                verify(roomMemberRepository, never()).save(any());
                assertThat(exitedMember.isExit()).isFalse();
            }

            @Test
            @DisplayName("[실패] 존재하지 않는 사용자면 USER_NOT_FOUND 예외")
            void shouldThrowWhenUserNotFound() {
                // given
                ChatRoom chatRoom = ChatRoom.builder().id(100L).build();

                given(roomMemberRepository.findByRoomIdAndUserId(100L, 999L))
                        .willReturn(Optional.empty());
                given(userRepository.findById(999L))
                        .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> service.registerGroupMember(chatRoom, 999L))
                        .isInstanceOf(BusinessException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);

                verify(roomMemberRepository, never()).save(any());
            }
        }



    }
}