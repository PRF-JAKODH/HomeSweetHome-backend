package com.homesweet.homesweetback.domain.chat.service.Imp;


import com.homesweet.homesweetback.common.exception.BusinessException;
import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.common.s3.impl.S3ImageUploader;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.repository.UserRepository;
import com.homesweet.homesweetback.domain.chat.dto.RoomDto;
import com.homesweet.homesweetback.domain.chat.dto.request.CreateGroupRoomRequest;
import com.homesweet.homesweetback.domain.chat.dto.response.GroupRoomCreateResponse;
import com.homesweet.homesweetback.domain.chat.entity.ChatRoom;
import com.homesweet.homesweetback.domain.chat.entity.RoomMember;
import com.homesweet.homesweetback.domain.chat.entity.enums.ChatRoomType;
import com.homesweet.homesweetback.domain.chat.entity.enums.ChatUserRole;
import com.homesweet.homesweetback.domain.chat.mapper.ChatRoomMapper;
import com.homesweet.homesweetback.domain.chat.repository.ChatRoomRepository;
import com.homesweet.homesweetback.domain.chat.repository.RoomMemberRepository;
import com.homesweet.homesweetback.domain.chat.service.RoomMemberService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

import static com.homesweet.homesweetback.domain.chat.entity.enums.ChatRoomType.GROUP;
import static com.homesweet.homesweetback.domain.chat.entity.enums.ChatRoomType.INDIVIDUAL;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 *
 * @author hygg0408e@gmail.com
 * @date 25. 11. 11.
 */
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
@DisplayName("[Service] 채팅방 서비스 단위 테스트")
public class ChatRoomServiceImplTest {

    @InjectMocks
    private ChatRoomServiceImpl service;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private RoomMemberRepository roomMemberRepository;

    @Mock
    private S3ImageUploader s3ImageUploader;

    @Mock
    private ChatRoomMapper chatRoomMapper;

    @Mock
    private RoomMemberService roomMemberService;

    @Nested
    @DisplayName("개인 채팅방 생성 테스트")
    class createOrGetIndividualRoom {


        @Nested
        @DisplayName("성공 케이스")
        class success {


            @Test
            @DisplayName("기존 채팅방이 있으면 재사용하고 DTO의 reused 필드는 true를 반환한다")
            void shouldReturnExistingRoomAndReusedTrue() {
                // given
                ChatRoom existingRoom = mock(ChatRoom.class);

                given(roomMemberService.buildPairKey(1L, 2L)).willReturn("1:2");
                given(existingRoom.getId()).willReturn(100L);
                given(chatRoomRepository.findByTypeAndPairKey(INDIVIDUAL, "1:2"))
                        .willReturn(Optional.of(existingRoom));

                // when
                RoomDto result = service.createOrGetIndividualRoom(1L, 2L);

                // then
                assertThat(result.roomId()).isEqualTo(100L);
                assertThat(result.reused()).isTrue();

                verify(chatRoomRepository, never()).saveAndFlush(any(ChatRoom.class));
                verify(roomMemberService, never()).registerIndividualMember(any(), anyLong(), anyLong());
            }


            @Test
            @DisplayName("기존 채팅방이 없으면 새 방을 생성한다.")
            void shouldCreateNewRoomAndReturnReusedFalse() {
                // given
                Long meId = 1L;
                Long targetId = 2L;
                String pairKey = "1:2";

                given(roomMemberService.buildPairKey(meId, targetId)).willReturn(pairKey);
                given(chatRoomRepository.findByTypeAndPairKey(ChatRoomType.INDIVIDUAL, pairKey))
                        .willReturn(Optional.empty());

                // saveAndFlush Mock
                given(chatRoomRepository.saveAndFlush(any(ChatRoom.class)))
                        .willAnswer(invocation -> {
                            ChatRoom room = invocation.getArgument(0);
                            ReflectionTestUtils.setField(room, "id", 100L);
                            return room;
                        });

                // when
                RoomDto result = service.createOrGetIndividualRoom(meId, targetId);

                // then
                assertThat(result.roomId()).isEqualTo(100L);
                assertThat(result.type()).isEqualTo("INDIVIDUAL");
                assertThat(result.name()).isEqualTo("INDIVIDUAL-1:2");
                assertThat(result.pairKey()).isEqualTo("1:2");
                assertThat(result.reused()).isFalse();

                verify(chatRoomRepository, times(1)).saveAndFlush(any(ChatRoom.class));
                verify(roomMemberService, times(1))
                        .registerIndividualMember(any(ChatRoom.class), eq(meId), eq(targetId));
            }
        }
    }

    @Nested
    @DisplayName("그룹 채팅방 생성 테스트")
    class createGroupRoom {

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("그룹 채팅방을 성공적으로 생성한다")
            void shouldCreateGroupRoom()  {
            // given
            MultipartFile thumbnailFile = mock(MultipartFile.class);        // mock 가짜 객체 생성 (인터페이스는 직접 생성 불가)
            String uploadedUrl = "https://s3.amazonaws.com/test.jpg";

            CreateGroupRoomRequest request = new CreateGroupRoomRequest(
                1L,
                "그룹방 제목",
                thumbnailFile
            );

            User owner = User.builder()
                    .id(1L)
                    .name("방장")
                    .build();

            ChatRoom chatRoom = ChatRoom.builder()
                    .id(100L)
                    .type(ChatRoomType.GROUP)
                    .name("테스트 그룹방")
                    .thumbnailUrl(uploadedUrl)
                    .build();

            GroupRoomCreateResponse expectedResponse = new GroupRoomCreateResponse(
                    100L,
                    1L,
                    "테스트 그룹방",
                    GROUP,
                    uploadedUrl
            );

            given(userRepository.findById(1L)).willReturn(Optional.of(owner));
            given(s3ImageUploader.upload(thumbnailFile, "group/chat/thumbnail")).willReturn(uploadedUrl);
            given(chatRoomMapper.toEntity(request, uploadedUrl)).willReturn(chatRoom);
            given(chatRoomMapper.toDto(chatRoom, 1L)).willReturn(expectedResponse);

            // when
            GroupRoomCreateResponse result = service.createGroupRoom(1L, request);

            // then
            assertThat(result.roomId()).isEqualTo(100L);
            assertThat(result.roomName()).isEqualTo("테스트 그룹방");
            assertThat(result.roomThumbnailUrl()).isEqualTo(uploadedUrl);
            assertThat(result.ownerId()).isEqualTo(1L);

            verify(userRepository).findById(1L);
            verify(s3ImageUploader).upload(thumbnailFile, "group/chat/thumbnail");
            verify(chatRoomRepository).saveAndFlush(chatRoom);
            verify(roomMemberRepository).save(any(RoomMember.class));
            }

            @Test
            @DisplayName("OWNER 역할로 방장을 등록한다")
            void shouldRegisterOwnerWithCorrectRole() {
                // given
                User owner = User.builder()
                        .id(1L)
                        .name("방장")
                        .build();

                ChatRoom chatRoom = ChatRoom.builder()
                        .id(100L)
                        .type(ChatRoomType.GROUP)
                        .name("테스트 그룹방")
                        .build();

                MultipartFile thumbnailFile = mock(MultipartFile.class);
                CreateGroupRoomRequest request = new CreateGroupRoomRequest(
                        1L,
                        "설명",
                        thumbnailFile
                );

                GroupRoomCreateResponse response = new GroupRoomCreateResponse(
                        100L, 1L, "테스트 그룹방", ChatRoomType.GROUP, "url"
                );

                given(userRepository.findById(1L)).willReturn(Optional.of(owner));
                given(s3ImageUploader.upload(any(), any())).willReturn("url");
                given(chatRoomMapper.toEntity(any(), any())).willReturn(chatRoom);
                given(chatRoomMapper.toDto(any(), any())).willReturn(response);

                ArgumentCaptor<RoomMember> captor = ArgumentCaptor.forClass(RoomMember.class);

                // when
                service.createGroupRoom(1L, request);

                // then
                verify(roomMemberRepository).save(captor.capture());
                RoomMember savedMember = captor.getValue();

                assertThat(savedMember.getRoom()).isEqualTo(chatRoom);
                assertThat(savedMember.getUser()).isEqualTo(owner);
                assertThat(savedMember.getRole()).isEqualTo(ChatUserRole.OWNER);
                assertThat(savedMember.getIsExit()).isFalse();
            }

        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("존재하지 않는 사용자면 USER_NOT_FOUND 예외가 발생한다.")
            void shouldThrowWhenUserNotFound() {
                    // given
                    given(userRepository.findById(999L)).willReturn(Optional.empty());

                    // when & then
                    assertThatThrownBy(() ->
                            service.createGroupRoom(999L, mock(CreateGroupRoomRequest.class)))
                            .isInstanceOf(BusinessException.class)
                            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);

                    verify(s3ImageUploader, never()).upload(any(), any());

            }

        }
    }

}



