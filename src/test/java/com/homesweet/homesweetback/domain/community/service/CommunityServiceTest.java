package com.homesweet.homesweetback.domain.community.service;

import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.repository.UserRepository;
import com.homesweet.homesweetback.domain.community.dto.*;
import com.homesweet.homesweetback.domain.community.exception.CommunityException;
import com.homesweet.homesweetback.domain.community.entity.*;
import com.homesweet.homesweetback.domain.search.community.event.CommunityEventPublisher;
import com.homesweet.homesweetback.domain.community.repository.*;
import com.homesweet.homesweetback.domain.notification.service.NotificationSendService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommunityServiceTest {

        @Mock
        private CommunityPostRepository postRepository;

        @Mock
        private CommunityCommentRepository commentRepository;

        @Mock
        private CommunityImageRepository imageRepository;

        @Mock
        private UserRepository userRepository;

        @Mock
        private NotificationSendService notificationSendService;

        @Mock
        private CommunityImageUploader imageUploader;

        @Mock
        private CommunityCountService communityCountService;

        @Mock
        private CommunityRedisService communityRedisService;

        @Mock
        private CommunityEventPublisher communityEventPublisher;

        @InjectMocks
        private CommunityPostService communityPostService;

        @InjectMocks
        private CommunityCommentService communityCommentService;

        @DisplayName("게시물 생성 테스트")
        @Test
        void postPost() {
                // given
                Long userId = 1L;
                CommunityPostRequest request = new CommunityPostRequest("Test Title", "Test Content", "Test category");

                User fakeUser = User.builder().id(userId).name("fakeUser").build();

                CommunityPostEntity savedPost = CommunityPostEntity.builder()
                                .postId(1L)
                                .author(fakeUser)
                                .title(request.title())
                                .content(request.content())
                                .category(request.category())
                                .build();

                when(userRepository.findById(userId)).thenReturn(Optional.of(fakeUser));
                when(postRepository.save(any(CommunityPostEntity.class))).thenReturn(savedPost);

                // when
                CommunityPostResponse response = communityPostService.createPost(Collections.emptyList(), request,
                                userId);

                // then
                assertThat(response).isNotNull();
                assertThat(response.title()).isEqualTo("Test Title");
                assertThat(response.content()).isEqualTo("Test Content");
                assertThat(response.category()).isEqualTo("Test category");

                verify(userRepository).findById(userId);
                verify(postRepository).save(any(CommunityPostEntity.class));
        }

        @DisplayName("게시물 조회 테스트")
        @Test
        void getPost() {
                // given
                Long postId = 1L;
                User fakeUser = User.builder().id(1L).name("fakeUser").build();
                CommunityPostEntity fakePost = CommunityPostEntity.builder()
                                .postId(postId)
                                .author(fakeUser)
                                .title("Test Title")
                                .content("Test Content")
                                .category("Test Category")
                                .viewCount(0)
                                .likeCount(0)
                                .commentCount(0)
                                .isModified(false)
                                .createdAt(java.time.LocalDateTime.now())
                                .modifiedAt(null)
                                .build();

                when(postRepository.findByPostIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(fakePost));
                when(imageRepository.findByPostOrderByImageOrderAsc(any(CommunityPostEntity.class)))
                                .thenReturn(Collections.emptyList());

                // Redis 캐시에서 카운트 조회를 Mock 처리
                when(communityCountService.getViewCountFromCache(postId)).thenReturn(5);
                when(communityCountService.getLikeCountFromCache(postId)).thenReturn(3);
                when(communityCountService.getCommentCountFromCache(postId)).thenReturn(2);

                // when
                CommunityPostResponse response = communityPostService.getPost(postId);

                // then
                assertThat(response).isNotNull();
                assertThat(response.postId()).isEqualTo(postId);
                assertThat(response.title()).isEqualTo("Test Title");
                assertThat(response.content()).isEqualTo("Test Content");
                assertThat(response.category()).isEqualTo("Test Category");
                assertThat(response.viewCount()).isEqualTo(5);
                assertThat(response.likeCount()).isEqualTo(3);
                assertThat(response.commentCount()).isEqualTo(2);

                verify(imageRepository).findByPostOrderByImageOrderAsc(any(CommunityPostEntity.class));
                verify(communityCountService).getViewCountFromCache(postId);
                verify(communityCountService).getLikeCountFromCache(postId);
                verify(communityCountService).getCommentCountFromCache(postId);
        }

        @DisplayName("게시물 수정 테스트")
        @Test
        void updatePost() {
                // given
                Long postId = 2L;
                Long userId = 2L;
                User fakeUser = User.builder().id(userId).name("User").build();
                CommunityPostEntity originalPost = CommunityPostEntity.builder()
                                .postId(postId)
                                .author(fakeUser)
                                .title("원본 제목")
                                .content("원본 내용")
                                .category("원본 카테고리")
                                .viewCount(0)
                                .likeCount(0)
                                .commentCount(0)
                                .isModified(false)
                                .createdAt(java.time.LocalDateTime.now())
                                .modifiedAt(null)
                                .build();

                CommunityPostRequest updateRequest = new CommunityPostRequest("수정된 제목", "수정된 내용", "수정된 카테고리");

                when(postRepository.findByPostIdAndIsDeletedFalse(postId)).thenReturn(Optional.of(originalPost));
                when(imageRepository.findByPostOrderByImageOrderAsc(any(CommunityPostEntity.class)))
                                .thenReturn(Collections.emptyList());

                // when
                CommunityPostResponse response = communityPostService.updatePost(postId, updateRequest, userId);

                // then
                assertThat(response).isNotNull();
                assertThat(response.title()).isEqualTo("수정된 제목");
                assertThat(response.content()).isEqualTo("수정된 내용");
                assertThat(response.category()).isEqualTo("수정된 카테고리");
                assertThat(response.isModified()).isTrue();

                verify(postRepository).findByPostIdAndIsDeletedFalse(postId);
        }

        @DisplayName("게시물 삭제 테스트")
        @Test
        void deletePost() {
                // given
                Long postId = 2L;
                Long userId = 2L;
                User fakeUser = User.builder().id(userId).name("User").build();
                CommunityPostEntity originalPost = CommunityPostEntity.builder()
                                .postId(postId)
                                .author(fakeUser)
                                .title("원본 제목")
                                .content("원본 내용")
                                .category("원본 카테고리")
                                .viewCount(0)
                                .likeCount(0)
                                .commentCount(0)
                                .isModified(false)
                                .createdAt(java.time.LocalDateTime.now())
                                .modifiedAt(null)
                                .build();

                when(postRepository.findByPostIdAndIsDeletedFalse(postId)).thenReturn(Optional.of(originalPost));

                // when
                communityPostService.deletePost(postId, userId);

                // then
                verify(postRepository).findByPostIdAndIsDeletedFalse(postId);
                assertThat(originalPost.getIsDeleted()).isTrue();
        }

        @DisplayName("게시물 목록 조회 테스트 (페이지네이션)")
        @Test
        void getPosts() {
                // given
                User fakeUser = User.builder().id(1L).name("User").build();

                CommunityPostEntity post1 = CommunityPostEntity.builder()
                                .postId(1L)
                                .author(fakeUser)
                                .title("첫 번째 게시물")
                                .content("첫 번째 내용")
                                .category("카테고리1")
                                .build();

                CommunityPostEntity post2 = CommunityPostEntity.builder()
                                .postId(2L)
                                .author(fakeUser)
                                .title("두 번째 게시물")
                                .content("두 번째 내용")
                                .category("카테고리2")
                                .build();

                Pageable pageable = PageRequest.of(0, 10);
                Page<CommunityPostEntity> page = new PageImpl<>(Arrays.asList(post1, post2), pageable, 2);

                when(postRepository.findByIsDeletedFalse(pageable)).thenReturn(page);
                when(imageRepository.findAllByPostInOrderByPostPostIdAscImageOrderAsc(anyList()))
                                .thenReturn(Collections.emptyList());

                // Redis 캐시에서 카운트 조회를 Mock 처리 (각 게시글마다)
                when(communityCountService.getViewCountFromCache(1L)).thenReturn(10);
                when(communityCountService.getLikeCountFromCache(1L)).thenReturn(5);
                when(communityCountService.getCommentCountFromCache(1L)).thenReturn(3);

                when(communityCountService.getViewCountFromCache(2L)).thenReturn(20);
                when(communityCountService.getLikeCountFromCache(2L)).thenReturn(8);
                when(communityCountService.getCommentCountFromCache(2L)).thenReturn(6);

                // when
                Page<CommunityPostResponse> result = communityPostService.getPosts(pageable);

                // then
                assertThat(result).isNotNull();
                assertThat(result.getContent()).hasSize(2);
                assertThat(result.getTotalElements()).isEqualTo(2);
                assertThat(result.getContent().get(0).title()).isEqualTo("첫 번째 게시물");
                assertThat(result.getContent().get(0).viewCount()).isEqualTo(10);
                assertThat(result.getContent().get(0).likeCount()).isEqualTo(5);
                assertThat(result.getContent().get(1).title()).isEqualTo("두 번째 게시물");
                assertThat(result.getContent().get(1).viewCount()).isEqualTo(20);
                assertThat(result.getContent().get(1).likeCount()).isEqualTo(8);

                verify(postRepository).findByIsDeletedFalse(pageable);
        }

        // ==================== 댓글 테스트 ====================

        @DisplayName("댓글 작성 테스트")
        @Test
        void createComment() {
                // given
                Long postId = 1L;
                Long userId = 1L;
                String commentContent = "테스트 댓글입니다.";

                User fakeUser = User.builder().id(userId).name("User").build();
                CommunityPostEntity fakePost = CommunityPostEntity.builder()
                                .postId(postId)
                                .author(fakeUser)
                                .title("Test Title")
                                .content("Test Content")
                                .category("Test Category")
                                .build();

                CommunityCommentRequest request = new CommunityCommentRequest(commentContent, null);

                CommunityCommentEntity savedComment = CommunityCommentEntity.builder()
                                .commentId(1L)
                                .post(fakePost)
                                .author(fakeUser)
                                .content(commentContent)
                                .build();

                when(userRepository.findById(userId)).thenReturn(Optional.of(fakeUser));
                when(postRepository.getReferenceById(postId)).thenReturn(fakePost);
                when(commentRepository.save(any(CommunityCommentEntity.class))).thenReturn(savedComment);

                // when
                CommunityCommentResponse response = communityCommentService.createComment(postId, request, userId);

                // then
                assertThat(response).isNotNull();
                assertThat(response.content()).isEqualTo(commentContent);
                assertThat(response.authorName()).isEqualTo("User");

                verify(userRepository).findById(userId);
                verify(communityCountService).increaseCommentCount(postId);
                verify(commentRepository).save(any(CommunityCommentEntity.class));
        }

        @DisplayName("댓글 수정 테스트")
        @Test
        void updateComment() {
                // given
                Long commentId = 1L;
                Long userId = 1L;
                String updatedContent = "수정된 댓글";

                User fakeUser = User.builder().id(userId).name("User").build();
                CommunityPostEntity fakePost = CommunityPostEntity.builder()
                                .postId(1L)
                                .author(fakeUser)
                                .build();

                CommunityCommentEntity comment = CommunityCommentEntity.builder()
                                .commentId(commentId)
                                .post(fakePost)
                                .author(fakeUser)
                                .content("원래 댓글")
                                .build();

                CommunityCommentRequest request = new CommunityCommentRequest(updatedContent, null);

                when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

                // when
                CommunityCommentResponse response = communityCommentService.updateComment(commentId, request, userId);

                // then
                assertThat(response).isNotNull();
                assertThat(response.content()).isEqualTo(updatedContent);

                verify(commentRepository).findById(commentId);
        }

        @DisplayName("댓글 삭제 테스트")
        @Test
        void deleteComment() {
                // given
                Long commentId = 1L;
                Long postId = 1L;
                Long userId = 1L;

                User fakeUser = User.builder().id(userId).name("User").build();
                CommunityPostEntity fakePost = CommunityPostEntity.builder()
                                .postId(postId)
                                .author(fakeUser)
                                .commentCount(1)
                                .build();

                CommunityCommentEntity comment = CommunityCommentEntity.builder()
                                .commentId(commentId)
                                .post(fakePost)
                                .author(fakeUser)
                                .content("삭제할 댓글")
                                .build();

                when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

                // when
                communityCommentService.deleteComment(commentId, postId, userId);

                // then
                assertThat(comment.getIsDeleted()).isTrue();
                verify(commentRepository).findById(commentId);
                verify(communityCountService).decreaseCommentCount(postId);
        }

        // Note: Redis 기반 카운트 테스트는 통합 테스트(CommunityCountServiceIntegratTest)와
        // 동시성 테스트(CommunityConcurrencyTest)에서 실제 Redis를 사용하여 수행합니다.

        // ==================== 예외 테스트 ====================

        @DisplayName("다른 사용자가 게시글 수정")
        @Test
        void otherUserUpdatePost() {
                // Given
                Long postId = 1L;
                Long authorId = 100L;
                Long otherUserId = 200L;

                User author = User.builder()
                                .id(authorId)
                                .name("User")
                                .build();

                CommunityPostEntity post = CommunityPostEntity.builder()
                                .postId(postId)
                                .author(author)
                                .build();

                CommunityPostRequest request = new CommunityPostRequest("수정 제목", "수정 내용", "카테고리");

                given(postRepository.findByPostIdAndIsDeletedFalse(postId))
                                .willReturn(Optional.of(post));

                // then
                assertThatThrownBy(() -> communityPostService.updatePost(postId, request, otherUserId))
                                .isInstanceOf(CommunityException.class)
                                .hasMessage("본인이 작성한 게시글만 수정/삭제할 수 있습니다")
                                .extracting("errorCode")
                                .isEqualTo(ErrorCode.COMMUNITY_POST_FORBIDDEN);
        }

        @DisplayName("존재하지 않는 게시글 조회")
        @Test
        void getPostNotFound() {
                // given
                Long nonExistPostId = 999L;

                given(postRepository.findByPostIdAndIsDeletedFalse(nonExistPostId))
                                .willReturn(Optional.empty());

                // then
                assertThatThrownBy(() -> communityPostService.getPost(nonExistPostId))
                                .isInstanceOf(CommunityException.class)
                                .hasMessage("해당하는 게시글을 찾을 수 없습니다")
                                .extracting("errorCode")
                                .isEqualTo(ErrorCode.COMMUNITY_POST_NOT_FOUND);
        }
}
