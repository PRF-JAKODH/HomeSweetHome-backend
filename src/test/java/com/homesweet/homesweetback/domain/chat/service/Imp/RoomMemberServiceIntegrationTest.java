package com.homesweet.homesweetback.domain.chat.service.Imp;

import com.homesweet.homesweetback.common.exception.BusinessException;
import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.auth.entity.OAuth2Provider;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.entity.UserRole;
import com.homesweet.homesweetback.domain.auth.repository.UserRepository;
import com.homesweet.homesweetback.domain.chat.dto.response.RoomMemberResponse;
import com.homesweet.homesweetback.domain.chat.entity.ChatRoom;
import com.homesweet.homesweetback.domain.chat.entity.RoomMember;
import com.homesweet.homesweetback.domain.chat.entity.enums.ChatRoomType;
import com.homesweet.homesweetback.domain.chat.entity.enums.ChatUserRole;
import com.homesweet.homesweetback.domain.chat.repository.jpa.ChatRoomRepository;
import com.homesweet.homesweetback.domain.chat.repository.jpa.RoomMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("RoomMemberService 통합 테스트")
class RoomMemberServiceIntegrationTest {

    @Autowired
    private RoomMemberServiceImpl roomMemberService;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoomMemberRepository roomMemberRepository;

    private ChatRoom testRoom;
    private User testUser;
    private RoomMember testMember;

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(
                User.builder()
                        .name("철수")
                        .email("chul@test.com")
                        .provider(OAuth2Provider.GOOGLE)
                        .role(UserRole.USER)
                        .profileImageUrl("profile.png")
                        .build()
        );

        testRoom = chatRoomRepository.save(
                ChatRoom.builder()
                        .name("테스트방")
                        .type(ChatRoomType.GROUP)
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        testMember = RoomMember.createMember(testRoom, testUser, ChatUserRole.MEMBER);
        testMember.exit();
        testMember = roomMemberRepository.save(testMember);

    }

    @Nested
    @DisplayName("그룹채팅방 신규 멤버 등록 테스트")
    class registerNewMember {

        @BeforeEach
        void clean() {
            roomMemberRepository.deleteAll();
        }

            @Test
            @DisplayName("[통합] 신규 멤버가 DB에 정상 등록된다")
            void registerNewMember_Integration() {

                // when
                RoomMemberResponse response = roomMemberService.registerNewMember(
                        testRoom.getId(), testUser.getId(), ChatUserRole.MEMBER);

                // then — DB에서 실제 조회
                Optional<RoomMember> saved = roomMemberRepository.findByRoomIdAndUserId(testRoom.getId(), testUser.getId());

                // 1) Optional 존재 여부 검증
                assertThat(saved).isPresent();

                // 2) 실제 엔티티 꺼내기
                RoomMember member = saved.get();

                // 3) 엔티티 필드 검증
                assertThat(member.getRoom().getId()).isEqualTo(testRoom.getId());
                assertThat(member.getUser().getId()).isEqualTo(testUser.getId());
                assertThat(member.getRole()).isEqualTo(ChatUserRole.MEMBER);
                assertThat(member.isExit()).isFalse();

                // DTO도 검증
                assertThat(response.userId()).isEqualTo(testUser.getId());
                assertThat(response.userName()).isEqualTo(testUser.getName());
                assertThat(response.profileUrl()).isEqualTo(testUser.getProfileImageUrl());
            }
    }


    @Nested
    @DisplayName("멤버 재입장 처리 테스트")
    class rejoinMember {

        @Test
        @DisplayName("[통합] DB 상태에서 exit 상태의 멤버가 재입장 처리된다")
        void rejoinMember_Integration_Success() {
            // when
            RoomMemberResponse response = roomMemberService.rejoinMember(
                    testRoom.getId(),
                    testUser.getId()
            );

            // then
            RoomMember updated = roomMemberRepository
                    .findByRoomIdAndUserId(testRoom.getId(), testUser.getId())
                    .orElseThrow();

            assertThat(updated.isExit()).isFalse();
            assertThat(response.userId()).isEqualTo(testUser.getId());
            assertThat(response.userName()).isEqualTo(testUser.getName());
            assertThat(response.profileUrl()).isEqualTo(testUser.getProfileImageUrl());
        }

        @Test
        @DisplayName("[통합] 존재하지 않는 멤버는 예외 발생")
        void rejoinMember_Integration_Fail_MemberNotFound() {
            // given: roomId or userId 조작
            Long wrongUserId = 999L;

            // when & then
            assertThatThrownBy(() ->
                    roomMemberService.rejoinMember(testRoom.getId(), wrongUserId)
            )
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(ErrorCode.ROOM_MEMBER_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("[통합] 이미 활성 상태면 예외 발생")
        void rejoinMember_Integration_Fail_AlreadyActive() {
            // given
            testMember.join(); // 활성 상태로 변경
            roomMemberRepository.save(testMember);

            // when & then
            assertThatThrownBy(() ->
                    roomMemberService.rejoinMember(testRoom.getId(), testUser.getId())
            )
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(ErrorCode.MEMBER_ALREADY_ACTIVE.getMessage());
        }

    }

}
