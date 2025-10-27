package com.homesweet.homesweetback.domain.community.service;

import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.repository.UserRepository;
import com.homesweet.homesweetback.domain.community.dto.CommunityPostRequest;
import com.homesweet.homesweetback.domain.community.dto.CommunityPostResponse;
import com.homesweet.homesweetback.domain.community.dto.exception.CommunityException;
import com.homesweet.homesweetback.domain.community.entity.CommunityImageEntity;
import com.homesweet.homesweetback.domain.community.entity.CommunityPostEntity;
import com.homesweet.homesweetback.domain.community.repository.CommunityImageRepository;
import com.homesweet.homesweetback.domain.community.repository.CommunityPostRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * Community 서비스 단위 테스트
 *
 * @author ohhalim777@gmail.com
 * @date 25. 10. 24.
 */
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
@DisplayName("커뮤니티 서비스 단위 테스트")
class CommunityServiceTest {

    @InjectMocks
    private CommunityPostService CommunityPostService;

    @Mock
    private CommunityPostRepository postRepository;

    @Mock
    private CommunityImageRepository imageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CommunityImageUploader imageUploader;

    @Nested
    @DisplayName("게시글 작성")
    class CreatePost {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("이미지 없이 게시글을 작성할 수 있다")
            void createPostWithoutImages() {
                // given
                Long userId = 1L;
                CommunityPostRequest request = new CommunityPostRequest(
                        "테스트 제목",
                        "테스트 내용입니다."
                );

                User mockUser = User.builder()
                        .id(userId)
                        .email("test@example.com")
                        .build();

                CommunityPostEntity mockPost = CommunityPostEntity.builder()
                        .postId(1L)
                        .author(mockUser)
                        .title(request.title())
                        .content(request.content())
                        .build();

                given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
                given(postRepository.save(any(CommunityPostEntity.class))).willReturn(mockPost);

                // when
                CommunityPostResponse response = CommunityPostService.createPost(null, request, userId);

                // then
                assertThat(response.title()).isEqualTo("테스트 제목");
                assertThat(response.content()).isEqualTo("테스트 내용입니다.");
                assertThat(response.imagesUrl()).isEmpty();

                verify(postRepository).save(any(CommunityPostEntity.class));
                verify(imageUploader, never()).uploadCommunityImages(any());
                verify(imageRepository, never()).save(any(CommunityImageEntity.class));
            }

            @Test
            @DisplayName("이미지 1장과 함께 게시글을 작성할 수 있다")
            void createPostWithSingleImage() {
                // given
                Long userId = 1L;
                CommunityPostRequest request = new CommunityPostRequest(
                        "이미지 포함 게시글",
                        "이미지가 포함된 내용입니다."
                );

                MultipartFile image = new MockMultipartFile(
                        "image",
                        "test.jpg",
                        "image/jpeg",
                        "test image content".getBytes()
                );
                List<MultipartFile> images = List.of(image);

                User mockUser = User.builder()
                        .id(userId)
                        .email("test@example.com")
                        .build();

                CommunityPostEntity mockPost = CommunityPostEntity.builder()
                        .postId(1L)
                        .author(mockUser)
                        .title(request.title())
                        .content(request.content())
                        .build();

                List<String> uploadedUrls = List.of("https://s3.aws/community/test.jpg");

                given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
                given(postRepository.save(any(CommunityPostEntity.class))).willReturn(mockPost);
                given(imageUploader.uploadCommunityImages(images)).willReturn(uploadedUrls);
                given(imageRepository.save(any(CommunityImageEntity.class)))
                        .willAnswer(invocation -> invocation.getArgument(0));

                // when
                CommunityPostResponse response = CommunityPostService.createPost(images, request, userId);

                // then
                assertThat(response.title()).isEqualTo("이미지 포함 게시글");
                assertThat(response.imagesUrl()).hasSize(1);
                assertThat(response.imagesUrl().get(0)).isEqualTo("https://s3.aws/community/test.jpg");

                verify(imageUploader).uploadCommunityImages(images);
                verify(imageRepository, times(1)).save(any(CommunityImageEntity.class));
            }

            @Test
            @DisplayName("이미지 여러 장과 함께 게시글을 작성할 수 있다")
            void createPostWithMultipleImages() {
                // given
                Long userId = 1L;
                CommunityPostRequest request = new CommunityPostRequest(
                        "다중 이미지 게시글",
                        "여러 이미지가 포함된 게시글입니다."
                );

                List<MultipartFile> images = List.of(
                        new MockMultipartFile("image1", "test1.jpg", "image/jpeg", "image1".getBytes()),
                        new MockMultipartFile("image2", "test2.jpg", "image/jpeg", "image2".getBytes()),
                        new MockMultipartFile("image3", "test3.jpg", "image/jpeg", "image3".getBytes())
                );

                User mockUser = User.builder()
                        .id(userId)
                        .email("test@example.com")
                        .build();

                CommunityPostEntity mockPost = CommunityPostEntity.builder()
                        .postId(1L)
                        .author(mockUser)
                        .title(request.title())
                        .content(request.content())
                        .build();

                List<String> uploadedUrls = List.of(
                        "https://s3.aws/community/test1.jpg",
                        "https://s3.aws/community/test2.jpg",
                        "https://s3.aws/community/test3.jpg"
                );

                given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
                given(postRepository.save(any(CommunityPostEntity.class))).willReturn(mockPost);
                given(imageUploader.uploadCommunityImages(images)).willReturn(uploadedUrls);
                given(imageRepository.save(any(CommunityImageEntity.class)))
                        .willAnswer(invocation -> invocation.getArgument(0));

                // when
                CommunityPostResponse response = CommunityPostService.createPost(images, request, userId);

                // then
                assertThat(response.title()).isEqualTo("다중 이미지 게시글");
                assertThat(response.imagesUrl()).hasSize(3);
                assertThat(response.imagesUrl()).containsExactly(
                        "https://s3.aws/community/test1.jpg",
                        "https://s3.aws/community/test2.jpg",
                        "https://s3.aws/community/test3.jpg"
                );

                verify(imageUploader).uploadCommunityImages(images);
                verify(imageRepository, times(3)).save(any(CommunityImageEntity.class));
            }

            @Test
            @DisplayName("이미지는 올바른 순서로 저장된다")
            void createPostWithImagesInCorrectOrder() {
                // given
                Long userId = 1L;
                CommunityPostRequest request = new CommunityPostRequest(
                        "순서 테스트",
                        "이미지 순서 확인"
                );

                List<MultipartFile> images = List.of(
                        new MockMultipartFile("first", "first.jpg", "image/jpeg", "first".getBytes()),
                        new MockMultipartFile("second", "second.jpg", "image/jpeg", "second".getBytes())
                );

                User mockUser = User.builder()
                        .id(userId)
                        .email("test@example.com")
                        .build();

                CommunityPostEntity mockPost = CommunityPostEntity.builder()
                        .postId(1L)
                        .author(mockUser)
                        .title(request.title())
                        .content(request.content())
                        .build();

                List<String> uploadedUrls = List.of(
                        "https://s3.aws/community/first.jpg",
                        "https://s3.aws/community/second.jpg"
                );

                given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
                given(postRepository.save(any(CommunityPostEntity.class))).willReturn(mockPost);
                given(imageUploader.uploadCommunityImages(images)).willReturn(uploadedUrls);
                given(imageRepository.save(any(CommunityImageEntity.class)))
                        .willAnswer(invocation -> invocation.getArgument(0));

                // when
                CommunityPostService.createPost(images, request, userId);

                // then
                ArgumentCaptor<CommunityImageEntity> captor = ArgumentCaptor.forClass(CommunityImageEntity.class);
                verify(imageRepository, times(2)).save(captor.capture());

                List<CommunityImageEntity> savedImages = captor.getAllValues();
                assertThat(savedImages.get(0).getImageOrder()).isEqualTo(0);
                assertThat(savedImages.get(0).getImageUrl()).isEqualTo("https://s3.aws/community/first.jpg");
                assertThat(savedImages.get(1).getImageOrder()).isEqualTo(1);
                assertThat(savedImages.get(1).getImageUrl()).isEqualTo("https://s3.aws/community/second.jpg");
            }
        }

        @Nested
        @DisplayName("실패")
        class Fail {

            @Test
            @DisplayName("존재하지 않는 사용자는 게시글을 작성할 수 없다")
            void createPostWithNonExistentUser() {
                // given
                Long userId = 999L;
                CommunityPostRequest request = new CommunityPostRequest(
                        "제목",
                        "내용"
                );

                given(userRepository.findById(userId)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() ->
                        CommunityPostService.createPost(null, request, userId)
                )
                        .isInstanceOf(CommunityException.class)
                        .hasMessage(ErrorCode.USER_NOT_FOUND.getMessage());

                verify(postRepository, never()).save(any(CommunityPostEntity.class));
            }
        }
    }

    @Nested
    @DisplayName("게시글 조회")
    class GetPost {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("게시글을 조회할 수 있다")
            void getPost() {
                // given
                Long postId = 1L;
                User mockUser = User.builder()
                        .id(1L)
                        .email("test@example.com")
                        .build();

                CommunityPostEntity mockPost = CommunityPostEntity.builder()
                        .postId(postId)
                        .author(mockUser)
                        .title("테스트 제목")
                        .content("테스트 내용")
                        .build();

                given(postRepository.findByPostIdAndIsDeletedFalse(postId))
                        .willReturn(Optional.of(mockPost));

                // when
                CommunityPostResponse response = CommunityPostService.getPost(postId);

                // then
                assertThat(response.postId()).isEqualTo(postId);
                assertThat(response.title()).isEqualTo("테스트 제목");
                assertThat(response.content()).isEqualTo("테스트 내용");

                verify(postRepository).findByPostIdAndIsDeletedFalse(postId);
            }
        }

        @Nested
        @DisplayName("실패")
        class Fail {

            @Test
            @DisplayName("존재하지 않는 게시글은 조회할 수 없다")
            void getPostNotFound() {
                // given
                Long postId = 999L;
                given(postRepository.findByPostIdAndIsDeletedFalse(postId))
                        .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() ->
                        CommunityPostService.getPost(postId)
                )
                        .isInstanceOf(CommunityException.class)
                        .hasMessage(ErrorCode.COMMUNITY_POST_NOT_FOUND.getMessage());

                verify(postRepository).findByPostIdAndIsDeletedFalse(postId);
            }
        }
    }

    @Nested
    @DisplayName("게시글 수정")
    class UpdatePost {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("작성자는 게시글을 수정할 수 있다")
            void updatePost() {
                // given
                Long postId = 1L;
                Long userId = 1L;
                CommunityPostRequest request = new CommunityPostRequest(
                        "수정된 제목",
                        "수정된 내용"
                );

                User mockUser = User.builder()
                        .id(userId)
                        .email("test@example.com")
                        .build();

                CommunityPostEntity mockPost = CommunityPostEntity.builder()
                        .postId(postId)
                        .author(mockUser)
                        .title("원래 제목")
                        .content("원래 내용")
                        .build();

                given(postRepository.findByPostIdAndIsDeletedFalse(postId))
                        .willReturn(Optional.of(mockPost));
                given(imageRepository.findByPostOrderByImageOrderAsc(mockPost))
                        .willReturn(List.of());

                // when
                CommunityPostResponse response = CommunityPostService.updatePost(postId, request, userId);

                // then
                assertThat(response.title()).isEqualTo("수정된 제목");
                assertThat(response.content()).isEqualTo("수정된 내용");
                assertThat(response.isModified()).isTrue();

                verify(postRepository).findByPostIdAndIsDeletedFalse(postId);
            }
        }

        @Nested
        @DisplayName("실패")
        class Fail {

            @Test
            @DisplayName("존재하지 않는 게시글은 수정할 수 없다")
            void updatePostNotFound() {
                // given
                Long postId = 999L;
                Long userId = 1L;
                CommunityPostRequest request = new CommunityPostRequest(
                        "수정된 제목",
                        "수정된 내용"
                );

                given(postRepository.findByPostIdAndIsDeletedFalse(postId))
                        .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() ->
                        CommunityPostService.updatePost(postId, request, userId)
                )
                        .isInstanceOf(CommunityException.class)
                        .hasMessage(ErrorCode.COMMUNITY_POST_NOT_FOUND.getMessage());

                verify(postRepository).findByPostIdAndIsDeletedFalse(postId);
            }

            @Test
            @DisplayName("작성자가 아니면 게시글을 수정할 수 없다")
            void updatePostForbidden() {
                // given
                Long postId = 1L;
                Long authorId = 1L;
                Long otherUserId = 2L;
                CommunityPostRequest request = new CommunityPostRequest(
                        "수정된 제목",
                        "수정된 내용"
                );

                User mockAuthor = User.builder()
                        .id(authorId)
                        .email("author@example.com")
                        .build();

                CommunityPostEntity mockPost = CommunityPostEntity.builder()
                        .postId(postId)
                        .author(mockAuthor)
                        .title("원래 제목")
                        .content("원래 내용")
                        .build();

                given(postRepository.findByPostIdAndIsDeletedFalse(postId))
                        .willReturn(Optional.of(mockPost));

                // when & then
                assertThatThrownBy(() ->
                        CommunityPostService.updatePost(postId, request, otherUserId)
                )
                        .isInstanceOf(CommunityException.class)
                        .hasMessage(ErrorCode.COMMUNITY_POST_FORBIDDEN.getMessage());

                verify(postRepository).findByPostIdAndIsDeletedFalse(postId);
            }
        }
    }

    @Nested
    @DisplayName("게시글 삭제")
    class DeletePost {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("작성자는 게시글을 삭제할 수 있다")
            void deletePost() {
                // given
                Long postId = 1L;
                Long userId = 1L;

                User mockUser = User.builder()
                        .id(userId)
                        .email("test@example.com")
                        .build();

                CommunityPostEntity mockPost = CommunityPostEntity.builder()
                        .postId(postId)
                        .author(mockUser)
                        .title("제목")
                        .content("내용")
                        .build();

                given(postRepository.findByPostIdAndIsDeletedFalse(postId))
                        .willReturn(Optional.of(mockPost));

                // when
                CommunityPostService.deletePost(postId, userId);

                // then
                verify(postRepository).findByPostIdAndIsDeletedFalse(postId);
            }
        }

        @Nested
        @DisplayName("실패")
        class Fail {

            @Test
            @DisplayName("존재하지 않는 게시글은 삭제할 수 없다")
            void deletePostNotFound() {
                // given
                Long postId = 999L;
                Long userId = 1L;

                given(postRepository.findByPostIdAndIsDeletedFalse(postId))
                        .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() ->
                        CommunityPostService.deletePost(postId, userId)
                )
                        .isInstanceOf(CommunityException.class)
                        .hasMessage(ErrorCode.COMMUNITY_POST_NOT_FOUND.getMessage());

                verify(postRepository).findByPostIdAndIsDeletedFalse(postId);
            }

            @Test
            @DisplayName("작성자가 아니면 게시글을 삭제할 수 없다")
            void deletePostForbidden() {
                // given
                Long postId = 1L;
                Long authorId = 1L;
                Long otherUserId = 2L;

                User mockAuthor = User.builder()
                        .id(authorId)
                        .email("author@example.com")
                        .build();

                CommunityPostEntity mockPost = CommunityPostEntity.builder()
                        .postId(postId)
                        .author(mockAuthor)
                        .title("제목")
                        .content("내용")
                        .build();

                given(postRepository.findByPostIdAndIsDeletedFalse(postId))
                        .willReturn(Optional.of(mockPost));

                // when & then
                assertThatThrownBy(() ->
                        CommunityPostService.deletePost(postId, otherUserId)
                )
                        .isInstanceOf(CommunityException.class)
                        .hasMessage(ErrorCode.COMMUNITY_POST_FORBIDDEN.getMessage());

                verify(postRepository).findByPostIdAndIsDeletedFalse(postId);
            }
        }
    }
}
