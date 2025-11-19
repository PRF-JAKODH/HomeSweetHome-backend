package com.homesweet.homesweetback.domain.chat.service.Imp;

import com.homesweet.homesweetback.common.exception.BusinessException;
import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.auth.entity.OAuth2Provider;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.entity.UserRole;
import com.homesweet.homesweetback.domain.auth.repository.UserRepository;
import com.homesweet.homesweetback.domain.chat.dto.RoomDto;
import com.homesweet.homesweetback.domain.chat.dto.request.CreateGroupRoomRequest;
import com.homesweet.homesweetback.domain.chat.dto.response.*;
import com.homesweet.homesweetback.domain.chat.entity.ChatMessage;
import com.homesweet.homesweetback.domain.chat.entity.ChatRoom;
import com.homesweet.homesweetback.domain.chat.entity.RoomMember;
import com.homesweet.homesweetback.domain.chat.entity.enums.ChatRoomType;
import com.homesweet.homesweetback.domain.chat.entity.enums.ChatUserRole;
import com.homesweet.homesweetback.domain.chat.entity.enums.MessageType;
import com.homesweet.homesweetback.domain.chat.repository.ChatMessageRepository;
import com.homesweet.homesweetback.domain.chat.repository.ChatRoomRepository;
import com.homesweet.homesweetback.domain.chat.repository.RoomMemberRepository;
import com.homesweet.homesweetback.domain.chat.service.ChatMessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.security.Provider;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("[Integration] 채팅방 서비스 통합 테스트")
class ChatRoomServiceIntegrationTest {

    @Autowired
    private ChatRoomServiceImpl chatRoomService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private RoomMemberRepository roomMemberRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private ChatMessageService chatMessageService;


    private User user1;
    private User user2;
    private User user3;

    MockMultipartFile thumbnailFile = new MockMultipartFile(
            "roomThumbnailUrl",              // 파라미터 이름
            "test-thumbnail.jpg",             // 원본 파일명
            "image/jpeg",                     // Content-Type
            "test image content".getBytes()   // 파일 내용
    );

    @BeforeEach
    void setUp() {
        // 테스트 유저 생성
        user1 = User.builder()
                .name("맹구씨")
                .email("user1@test.com")
                .profileImageUrl("https://test.com/user1.jpg")
                .provider(OAuth2Provider.GOOGLE)
                .role(UserRole.USER)
                .build();
        user1 = userRepository.save(user1);

        user2 = User.builder()
                .name("짱구씨")
                .email("user2@test.com")
                .profileImageUrl("https://test.com/user2.jpg")
                .provider(OAuth2Provider.GOOGLE)
                .role(UserRole.USER)
                .build();
        user2 = userRepository.save(user2);

        user3 = User.builder()
                .name("철수씨")
                .email("user3@test.com")
                .profileImageUrl("https://test.com/user3.jpg")
                .provider(OAuth2Provider.GOOGLE)
                .role(UserRole.USER)
                .build();
        user3 = userRepository.save(user3);

    }

    @Nested
    @DisplayName("개인 채팅방 생성 테스트")
    class CreateIndividualRoomTest {

        @Test
        @DisplayName("[성공] 새로운 개인 채팅방을 생성한다")
        void createIndividualRoom_NewRoom_Success() {
            // when
            RoomDto result = chatRoomService.createOrGetIndividualRoom(user1.getId(), user2.getId());

            // then
            assertThat(result).isNotNull();
            assertThat(result.roomId()).isNotNull();
            assertThat(result.reused()).isFalse();
            assertThat(result.type()).isEqualTo(ChatRoomType.INDIVIDUAL.name());

            // DB 검증
            ChatRoom savedRoom = chatRoomRepository.findById(result.roomId()).orElseThrow();
            assertThat(savedRoom.getType()).isEqualTo(ChatRoomType.INDIVIDUAL);
            assertThat(savedRoom.getPairKey()).isNotNull();

            // 멤버 검증
            List<RoomMember> members = roomMemberRepository.findByRoom_IdAndIsExitFalse(result.roomId());
            assertThat(members).hasSize(2);
        }

        @Test
        @DisplayName("[성공] 기존 개인 채팅방이 있으면 재사용한다")
        void createIndividualRoom_ExistingRoom_Reused() {
            // given - 기존 방 생성
            RoomDto firstRoom = chatRoomService.createOrGetIndividualRoom(user1.getId(), user2.getId());

            // when - 같은 사용자로 다시 생성 요청
            RoomDto secondRoom = chatRoomService.createOrGetIndividualRoom(user1.getId(), user2.getId());

            // then
            assertThat(secondRoom.roomId()).isEqualTo(firstRoom.roomId());
            assertThat(secondRoom.reused()).isTrue();

            // 방이 1개만 존재하는지 확인
            List<ChatRoom> rooms = chatRoomRepository.findByType(ChatRoomType.INDIVIDUAL);
            long matchingRooms = rooms.stream()
                    .filter(r -> r.getId().equals(firstRoom.roomId()))
                    .count();
            assertThat(matchingRooms).isEqualTo(1);
        }

        @Test
        @DisplayName("[성공] 순서가 바뀌어도 같은 채팅방으로 인식한다")
        void createIndividualRoom_ReversedOrder_SameRoom() {
            // given
            RoomDto room1 = chatRoomService.createOrGetIndividualRoom(user1.getId(), user2.getId());

            // when - 순서를 바꿔서 생성
            RoomDto room2 = chatRoomService.createOrGetIndividualRoom(user2.getId(), user1.getId());

            // then
            assertThat(room2.roomId()).isEqualTo(room1.roomId());
            assertThat(room2.reused()).isTrue();
        }
    }

    @Nested
    @DisplayName("그룹 채팅방 생성 테스트")
    class CreateGroupRoomTest {

        @Test
        @DisplayName("[성공] 그룹 채팅방을 생성한다")
        void createGroupRoom_Success() {
            // given

            CreateGroupRoomRequest request = new CreateGroupRoomRequest(
                    1L,
                    "테스트 그룹방",
                    thumbnailFile,
                    ChatRoomType.GROUP
            );

            // when
            GroupRoomCreateResponse response = chatRoomService.createGroupRoom(user1.getId(), request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.roomId()).isNotNull();
            assertThat(response.roomName()).isEqualTo("테스트 그룹방");
            assertThat(response.type()).isEqualTo(ChatRoomType.GROUP);
            assertThat(response.ownerId()).isEqualTo(user1.getId());

            // DB 검증
            ChatRoom savedRoom = chatRoomRepository.findById(response.roomId()).orElseThrow();
            assertThat(savedRoom.getType()).isEqualTo(ChatRoomType.GROUP);
            assertThat(savedRoom.getName()).isEqualTo("테스트 그룹방");

            // 방장 검증
            RoomMember owner = roomMemberRepository
                    .findByRoomIdAndUserId(response.roomId(), user1.getId())
                    .orElseThrow();
            assertThat(owner.getRole()).isEqualTo(ChatUserRole.OWNER);
            assertThat(owner.isExit()).isFalse();
        }

        @Test
        @DisplayName("[실패] 방 타입이 GROUP이 아니면 예외가 발생한다")
        void createGroupRoom_InvalidType_ThrowsException() {
            //given

            CreateGroupRoomRequest request = new CreateGroupRoomRequest(
                    1L,
                    "테스트 그룹방",
                    thumbnailFile,
                    ChatRoomType.INDIVIDUAL
            );

            // when & then
            assertThatThrownBy(() ->
                    chatRoomService.createGroupRoom(user1.getId(), request)
            )
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_ROOM_TYPE);
        }

        @Test
        @DisplayName("[실패] 존재하지 않는 사용자면 예외가 발생한다")
        void createGroupRoom_UserNotFound_ThrowsException() {
            // given
            Long invalidUserId = 999L;

            CreateGroupRoomRequest request = new CreateGroupRoomRequest(
                    1L,
                    "테스트 그룹방",
                    thumbnailFile,
                    ChatRoomType.GROUP
            );

            // when & then
            assertThatThrownBy(() ->
                    chatRoomService.createGroupRoom(invalidUserId, request)
            )
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("개인 채팅방 상세 조회 테스트")
    class GetIndividualChatDetailTest {

        @Test
        @DisplayName("[성공] 개인 채팅방 상세 정보를 조회한다")
        void getIndividualChatDetail_Success() {
            // given
            RoomDto room = chatRoomService.createOrGetIndividualRoom(user1.getId(), user2.getId());

            // when
            IndividualChatDetailResponse response = chatRoomService.getIndividualChatDetail(
                    user1.getId(), room.roomId()
            );

            // then
            assertThat(response).isNotNull();
            assertThat(response.roomId()).isEqualTo(room.roomId());
            assertThat(response.partnerId()).isEqualTo(user2.getId());
            assertThat(response.partnerName()).isEqualTo("짱구씨");
            assertThat(response.partnerProfileImageUrl()).isEqualTo("https://test.com/user2.jpg");
        }

        @Test
        @DisplayName("[성공] 상대방 입장에서도 정보를 정확히 조회한다")
        void getIndividualChatDetail_PartnerPerspective_Success() {
            // given
            RoomDto room = chatRoomService.createOrGetIndividualRoom(user1.getId(), user2.getId());

            // when - user2 입장에서 조회
            IndividualChatDetailResponse response = chatRoomService.getIndividualChatDetail(
                    user2.getId(), room.roomId()
            );

            // then
            assertThat(response.partnerId()).isEqualTo(user1.getId());
            assertThat(response.partnerName()).isEqualTo("맹구씨");
        }

        @Test
        @DisplayName("[실패] 존재하지 않는 채팅방이면 예외가 발생한다")
        void getIndividualChatDetail_RoomNotFound_ThrowsException() {
            // given
            Long invalidRoomId = 999L;

            // when & then
            assertThatThrownBy(() ->
                    chatRoomService.getIndividualChatDetail(user1.getId(), invalidRoomId)
            )
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROOM_NOT_FOUND);
        }

        @Test
        @DisplayName("[실패] 그룹 채팅방을 개인 채팅방으로 조회하면 예외가 발생한다")
        void getIndividualChatDetail_WrongRoomType_ThrowsException() {
            // given
            CreateGroupRoomRequest request = new CreateGroupRoomRequest(
                    1L,
                    "그룹방", thumbnailFile , ChatRoomType.GROUP
            );
            GroupRoomCreateResponse groupRoom = chatRoomService.createGroupRoom(user1.getId(), request);

            // when & then
            assertThatThrownBy(() ->
                    chatRoomService.getIndividualChatDetail(user1.getId(), groupRoom.roomId())
            )
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_ROOM_TYPE);
        }
    }

    @Nested
    @DisplayName("그룹 채팅방 상세 조회 테스트")
    class GetGroupChatDetailTest {

        @Test
        @DisplayName("[성공] 그룹 채팅방 상세 정보를 조회한다")
        void getGroupChatDetail_Success() {
            // given
            CreateGroupRoomRequest request = new CreateGroupRoomRequest(
                    1L,"테스트 그룹방", thumbnailFile, ChatRoomType.GROUP
            );
            GroupRoomCreateResponse createdRoom = chatRoomService.createGroupRoom(user1.getId(), request);

            // when
            GroupChatDetailResponse response = chatRoomService.getGroupChatDetail(
                    user1.getId(), createdRoom.roomId()
            );

            // then
            assertThat(response).isNotNull();
            assertThat(response.roomId()).isEqualTo(createdRoom.roomId());
            assertThat(response.roomName()).isEqualTo("테스트 그룹방");
            assertThat(response.roomType()).isEqualTo(ChatRoomType.GROUP);
            assertThat(response.memberCount()).isEqualTo(1);
            assertThat(response.participants()).hasSize(1);
        }

        @Test
        @DisplayName("[성공] 새로운 멤버가 조회하면 자동으로 등록된다")
        void getGroupChatDetail_NewMember_AutoRegistered() {
            // given
            CreateGroupRoomRequest request = new CreateGroupRoomRequest(
                   1L, "테스트 그룹방", thumbnailFile, ChatRoomType.GROUP
            );
            GroupRoomCreateResponse createdRoom = chatRoomService.createGroupRoom(user1.getId(), request);

            // when - user2가 처음 조회
            GroupChatDetailResponse response = chatRoomService.getGroupChatDetail(
                    user2.getId(), createdRoom.roomId()
            );

            // then
            assertThat(response.memberCount()).isEqualTo(2);
            assertThat(response.participants()).hasSize(2);

            // DB 검증
            RoomMember newMember = roomMemberRepository
                    .findByRoomIdAndUserId(createdRoom.roomId(), user2.getId())
                    .orElseThrow();
            assertThat(newMember.getRole()).isEqualTo(ChatUserRole.MEMBER);
            assertThat(newMember.isExit()).isFalse();
        }

        @Test
        @DisplayName("[성공] 퇴장한 멤버가 재입장하면 is_exit이 false로 변경된다")
        void getGroupChatDetail_ReEnter_Success() {
            // given
            CreateGroupRoomRequest request = new CreateGroupRoomRequest(
                    3L, "테스트 그룹방", thumbnailFile, ChatRoomType.GROUP
            );
            GroupRoomCreateResponse createdRoom = chatRoomService.createGroupRoom(user1.getId(), request);

            // user2가 입장했다가 퇴장
            chatRoomService.getGroupChatDetail(user2.getId(), createdRoom.roomId());
            chatRoomService.exitRoom(user2.getId(), createdRoom.roomId());

            // when - user2가 다시 조회 (재입장)
            GroupChatDetailResponse response = chatRoomService.getGroupChatDetail(
                    user2.getId(), createdRoom.roomId()
            );

            // then
            assertThat(response.memberCount()).isEqualTo(2);

            RoomMember reenteredMember = roomMemberRepository
                    .findByRoomIdAndUserId(createdRoom.roomId(), user2.getId())
                    .orElseThrow();
            assertThat(reenteredMember.isExit()).isFalse();
        }

        @Test
        @DisplayName("[실패] 개인 채팅방을 그룹 채팅방으로 조회하면 예외가 발생한다")
        void getGroupChatDetail_WrongRoomType_ThrowsException() {
            // given
            RoomDto individualRoom = chatRoomService.createOrGetIndividualRoom(
                    user1.getId(), user2.getId()
            );

            // when & then
            assertThatThrownBy(() ->
                    chatRoomService.getGroupChatDetail(user1.getId(), individualRoom.roomId())
            )
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_ROOM_TYPE);
        }
    }

    @Nested
    @DisplayName("채팅방 퇴장 테스트")
    class ExitRoomTest {

        @Test
        @DisplayName("[성공] 개인 채팅방에서 퇴장한다")
        void exitRoom_IndividualRoom_Success() {
            // given
            RoomDto room = chatRoomService.createOrGetIndividualRoom(user1.getId(), user2.getId());

            // when
            chatRoomService.exitRoom(user1.getId(), room.roomId());

            // then
            RoomMember exitedMember = roomMemberRepository
                    .findByRoomIdAndUserId(room.roomId(), user1.getId())
                    .orElseThrow();
            assertThat(exitedMember.isExit()).isTrue();

            // 채팅방은 여전히 존재
            ChatRoom chatRoom = chatRoomRepository.findById(room.roomId()).orElseThrow();
            assertThat(chatRoom.getIsDeleted()).isFalse();
        }

        @Test
        @DisplayName("[성공] 그룹 채팅방에서 퇴장한다")
        void exitRoom_GroupRoom_Success() {
            // given
            CreateGroupRoomRequest request = new CreateGroupRoomRequest(
                   1L, "테스트 그룹방", thumbnailFile, ChatRoomType.GROUP
            );
            GroupRoomCreateResponse createdRoom = chatRoomService.createGroupRoom(user1.getId(), request);
            chatRoomService.getGroupChatDetail(user2.getId(), createdRoom.roomId());

            // when
            chatRoomService.exitRoom(user2.getId(), createdRoom.roomId());

            // then
            RoomMember exitedMember = roomMemberRepository
                    .findByRoomIdAndUserId(createdRoom.roomId(), user2.getId())
                    .orElseThrow();
            assertThat(exitedMember.isExit()).isTrue();
        }

        @Test
        @DisplayName("[성공] 그룹 채팅방의 마지막 멤버가 퇴장하면 방이 삭제된다")
        void exitRoom_LastMemberExits_RoomDeleted() {
            // given
            CreateGroupRoomRequest request = new CreateGroupRoomRequest(
                    1L, "테스트 그룹방", thumbnailFile, ChatRoomType.GROUP
            );
            GroupRoomCreateResponse createdRoom = chatRoomService.createGroupRoom(user1.getId(), request);

            // when - 방장이 퇴장 (마지막 멤버)
            chatRoomService.exitRoom(user1.getId(), createdRoom.roomId());

            // then
            ChatRoom deletedRoom = chatRoomRepository.findById(createdRoom.roomId()).orElseThrow();
            assertThat(deletedRoom.getIsDeleted()).isNotNull();
        }

        @Test
        @DisplayName("[실패] 존재하지 않는 채팅방이면 예외가 발생한다")
        void exitRoom_RoomNotFound_ThrowsException() {
            // given
            Long invalidRoomId = 999L;

            // when & then
            assertThatThrownBy(() ->
                    chatRoomService.exitRoom(user1.getId(), invalidRoomId)
            )
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROOM_NOT_FOUND);
        }

        @Test
        @DisplayName("[실패] 채팅방 멤버가 아니면 예외가 발생한다")
        void exitRoom_NotMember_ThrowsException() {
            // given
            RoomDto room = chatRoomService.createOrGetIndividualRoom(user1.getId(), user2.getId());

            // when & then - user3는 멤버가 아님
            assertThatThrownBy(() ->
                    chatRoomService.exitRoom(user3.getId(), room.roomId())
            )
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROOM_MEMBER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("채팅방 목록 조회 테스트")
    class FindMyRoomsTest {

        @Test
        @DisplayName("[성공] 내가 속한 개인 채팅방 목록을 조회한다")
        void findMyIndividualRooms_Success() {
            // given
            chatRoomService.createOrGetIndividualRoom(user1.getId(), user2.getId());
            chatRoomService.createOrGetIndividualRoom(user1.getId(), user3.getId());

            // when
            List<IndividualRoomListResponse> rooms = chatRoomService.findMyIndividualRooms(user1.getId());

            // then
            assertThat(rooms).hasSize(2);
        }

        @Test
        @DisplayName("[성공] 내가 속한 그룹 채팅방 목록을 조회한다")
        void findMyGroupRooms_Success() {
            // given
            CreateGroupRoomRequest request1 = new CreateGroupRoomRequest(
                    1L,"그룹방1", thumbnailFile, ChatRoomType.GROUP
            );
            CreateGroupRoomRequest request2 = new CreateGroupRoomRequest(
                    2L,"그룹방2", thumbnailFile, ChatRoomType.GROUP
            );
            chatRoomService.createGroupRoom(user1.getId(), request1);
            chatRoomService.createGroupRoom(user1.getId(), request2);

            // when
            List<GroupRoomListResponse> rooms = chatRoomService.findMyGroupRooms(user1.getId());

            // then
            assertThat(rooms).hasSize(2);
        }

        @Test
        @DisplayName("[성공] 퇴장한 채팅방은 목록에 포함되지 않는다")
        void findMyRooms_ExcludesExitedRooms() {
            // given
            RoomDto room1 = chatRoomService.createOrGetIndividualRoom(user1.getId(), user2.getId());
            chatRoomService.createOrGetIndividualRoom(user1.getId(), user3.getId());

            // room1에서 퇴장
            chatRoomService.exitRoom(user1.getId(), room1.roomId());

            // when
            List<IndividualRoomListResponse> rooms = chatRoomService.findMyIndividualRooms(user1.getId());

            // then
            assertThat(rooms).hasSize(1);
        }
    }

    @Nested
    @DisplayName("전체 그룹 채팅방 조회 테스트")
    class GetAllGroupRoomsTest {

        @Test
        @DisplayName("[성공] 모든 그룹 채팅방을 조회한다")
        void getAllGroupRooms_Success() {
            // given
            CreateGroupRoomRequest request1 = new CreateGroupRoomRequest(
                    1L, "그룹방1", thumbnailFile, ChatRoomType.GROUP
            );
            CreateGroupRoomRequest request2 = new CreateGroupRoomRequest(
                    2L,"그룹방2", thumbnailFile, ChatRoomType.GROUP
            );
            chatRoomService.createGroupRoom(user1.getId(), request1);
            chatRoomService.createGroupRoom(user2.getId(), request2);

            // when
            List<GroupRoomListResponse> rooms = chatRoomService.getAllGroupRooms();

            // then
            assertThat(rooms).hasSize(2);
            assertThat(rooms.get(0).roomName()).isIn("그룹방1", "그룹방2");
        }

        @Test
        @DisplayName("[성공] 마지막 메시지 정보가 포함된다")
        void getAllGroupRooms_IncludesLastMessage() {
            // given
            CreateGroupRoomRequest request = new CreateGroupRoomRequest(
                    1L, "그룹방", thumbnailFile, ChatRoomType.GROUP
            );
            GroupRoomCreateResponse room = chatRoomService.createGroupRoom(user1.getId(), request);

            chatMessageService.sendMessage(room.roomId(), user1.getId(), "마지막 메시지");

            // 메시지 생성
            ChatRoom chatRoom = chatRoomRepository.findById(room.roomId()).orElseThrow();
            ChatMessage message = ChatMessage.builder()
                    .room(chatRoom)
                    .sender(user1)
                    .content("마지막 메시지")
                    .messageType(MessageType.TEXT)
                    .sentAt(LocalDateTime.now())
                    .build();
            chatMessageRepository.save(message);

            // when
            List<GroupRoomListResponse> rooms = chatRoomService.getAllGroupRooms();

            // then
            assertThat(rooms).hasSize(1);
            assertThat(rooms.get(0).lastMessage()).isEqualTo("마지막 메시지");
        }
    }

    @Nested
    @DisplayName("사용자 채팅방 참여 확인 테스트")
    class IsUserInRoomTest {

//        @Test
//        @DisplayName("[성공] 사용자가 채팅방에 참여 중이면 true를 반환한다")
//        void isUserInRoom_UserIsInRoom_ReturnsTrue() {
//            // given
//            RoomDto room = chatRoomService.createOrGetIndividualRoom(user1.getId(), user2.getId());
//            System.out.println("============== room " + room + "==============");
//
//            // when
//            boolean result = chatRoomService.isUserInRoom(user1.getId(), room.roomId());
//
//            // then
//
//            System.out.println("============== result " + result + "==============");
//            assertThat(result).isTrue();
//        }

        @Test
        @DisplayName("[성공] 사용자가 채팅방에 없으면 false를 반환한다")
        void isUserInRoom_UserNotInRoom_ReturnsFalse() {
            // given
            RoomDto room = chatRoomService.createOrGetIndividualRoom(user1.getId(), user2.getId());

            // when
            boolean result = chatRoomService.isUserInRoom(user3.getId(), room.roomId());

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("[성공] 퇴장한 사용자는 false를 반환한다")
        void isUserInRoom_ExitedUser_ReturnsFalse() {
            // given
            RoomDto room = chatRoomService.createOrGetIndividualRoom(user1.getId(), user2.getId());
            chatRoomService.exitRoom(user1.getId(), room.roomId());

            // when
            boolean result = chatRoomService.isUserInRoom(user1.getId(), room.roomId());

            // then
            assertThat(result).isFalse();
        }
    }
}