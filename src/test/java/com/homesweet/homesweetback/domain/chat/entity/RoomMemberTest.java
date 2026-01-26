package com.homesweet.homesweetback.domain.chat.entity;

import com.homesweet.homesweetback.domain.auth.entity.OAuth2Provider;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.entity.UserRole;
import com.homesweet.homesweetback.domain.chat.entity.enums.ChatRoomType;
import com.homesweet.homesweetback.domain.chat.entity.enums.ChatUserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DisplayName("RoomMember 엔티티 테스트")
class RoomMemberTest {

    private User testUser;
    private ChatRoom testRoom;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .name("맹구")
                .email("maenggu@test.com")
                .provider(OAuth2Provider.GOOGLE)
                .role(UserRole.USER)
                .profileImageUrl("test.png")
                .build();

        testRoom = ChatRoom.builder()
                .type(ChatRoomType.INDIVIDUAL)
                .name("테스트방")
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ===== exit() 메서드 테스트 =====

    @Test
    @DisplayName("exit() 호출 시 isExit이 true로 변경된다")
    void exit_shouldSetIsExitTrue() {
        // Given
        RoomMember member = RoomMember.createMember(testRoom, testUser, ChatUserRole.MEMBER);
        assertThat(member.isExit()).isFalse();

        // When
        member.exit();

        // Then
        assertThat(member.isExit()).isTrue();
    }

    @Test
    @DisplayName("생성 시 기본값으로 isExit은 false이다")
    void constructor_shouldSetIsExitFalse() {
        // Given & When
        RoomMember member = RoomMember.createMember(testRoom, testUser, ChatUserRole.MEMBER);

        // Then
        assertThat(member.isExit()).isFalse();
    }

    @Test
    @DisplayName("이미 퇴장한 상태에서 exit() 재호출 시에도 isExit은 true를 유지한다")
    void exit_whenAlreadyExited_shouldRemainTrue() {
        // Given
        RoomMember member = RoomMember.createMember(testRoom, testUser, ChatUserRole.MEMBER);
        assertThat(member.isExit()).isFalse();

        member.exit(); // 첫 번째 퇴장

        // When
        member.exit(); // 두 번째 퇴장 시도

        // Then
        assertThat(member.isExit()).isTrue();
    }

    // ===== createMember() 정적 팩토리 메서드 테스트 =====

    @Test
    @DisplayName("createMember() - role이 주어지면 해당 role로 생성된다")
    void createMember_withRole_shouldCreateWithGivenRole() {
        // Given
        ChatUserRole givenRole = ChatUserRole.OWNER;

        // When
        RoomMember member = RoomMember.createMember(testRoom, testUser, givenRole);

        // Then
        assertThat(member.getRoom()).isEqualTo(testRoom);
        assertThat(member.getUser()).isEqualTo(testUser);
        assertThat(member.getRole()).isEqualTo(ChatUserRole.OWNER);
        assertThat(member.isExit()).isFalse();
    }

    @Test
    @DisplayName("createMember() - role이 null이면 기본값 MEMBER로 생성된다")
    void createMember_withNullRole_shouldCreateWithDefaultMemberRole() {
        // Given
        ChatUserRole nullRole = null;

        // When
        RoomMember member = RoomMember.createMember(testRoom, testUser, nullRole);

        // Then
        assertThat(member.getRoom()).isEqualTo(testRoom);
        assertThat(member.getUser()).isEqualTo(testUser);
        assertThat(member.getRole()).isEqualTo(ChatUserRole.MEMBER); // 기본값
        assertThat(member.isExit()).isFalse();
    }

    @Test
    @DisplayName("createMember() - isExit은 항상 false로 초기화된다")
    void createMember_shouldAlwaysInitializeIsExitAsFalse() {
        // Given & When
        RoomMember memberWithAdmin = RoomMember.createMember(testRoom, testUser, ChatUserRole.OWNER);
        RoomMember memberWithNull = RoomMember.createMember(testRoom, testUser, null);

        // Then
        assertThat(memberWithAdmin.isExit()).isFalse();
        assertThat(memberWithNull.isExit()).isFalse();
    }


    @Test
    @DisplayName("createMember() - 모든 ChatUserRole에 대해 정상 생성")
    void createMember_withAllRoles_shouldCreateSuccessfully() {
        // Given & When & Then
        for (ChatUserRole role : ChatUserRole.values()) {
            RoomMember member = RoomMember.createMember(testRoom, testUser, role);

            assertThat(member.getRole()).isEqualTo(role);
            assertThat(member.getRoom()).isEqualTo(testRoom);
            assertThat(member.getUser()).isEqualTo(testUser);
            assertThat(member.isExit()).isFalse();
        }
    }

    @Test
    @DisplayName("createMember() - 생성된 멤버는 room과 user를 정확히 참조한다")
    void createMember_shouldReferenceCorrectRoomAndUser() {
        // Given
        User anotherUser = User.builder()
                .name("철수")
                .email("chulsoo@test.com")
                .provider(OAuth2Provider.KAKAO)
                .role(UserRole.USER)
                .build();

        ChatRoom anotherRoom = ChatRoom.builder()
                .type(ChatRoomType.GROUP)
                .name("다른방")
                .createdAt(LocalDateTime.now())
                .build();

        // When
        RoomMember member1 = RoomMember.createMember(testRoom, testUser, ChatUserRole.OWNER);
        RoomMember member2 = RoomMember.createMember(anotherRoom, anotherUser, ChatUserRole.MEMBER);

        // Then
        assertThat(member1.getRoom()).isEqualTo(testRoom);
        assertThat(member1.getUser()).isEqualTo(testUser);
        assertThat(member1.getRoom()).isNotEqualTo(anotherRoom);
        assertThat(member1.getUser()).isNotEqualTo(anotherUser);

        assertThat(member2.getRoom()).isEqualTo(anotherRoom);
        assertThat(member2.getUser()).isEqualTo(anotherUser);
    }
}