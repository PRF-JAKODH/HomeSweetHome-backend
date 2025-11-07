package com.homesweet.homesweetback.domain.community.service;

import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.repository.UserRepository;
import com.homesweet.homesweetback.domain.community.dto.CommunityCommentRequest;
import com.homesweet.homesweetback.domain.community.dto.CommunityPostRequest;
import com.homesweet.homesweetback.domain.community.dto.CommunityPostResponse;
import com.homesweet.homesweetback.domain.community.dto.exception.CommunityException;
import com.homesweet.homesweetback.domain.community.entity.CommunityCommentEntity;
import com.homesweet.homesweetback.domain.community.entity.CommunityPostEntity;
import com.homesweet.homesweetback.domain.community.repository.CommunityCommentRepository;
import com.homesweet.homesweetback.domain.community.repository.CommunityImageRepository;
import com.homesweet.homesweetback.domain.community.repository.CommunityPostRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
    @InjectMocks
    private CommunityPostService communityPostService;

    @InjectMocks
    private CommunityCommentService communityCommentService;

    @InjectMocks
    private CommunityCountService communityCountService;

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
        CommunityPostResponse response = communityPostService.createPost(Collections.emptyList(), request, userId);

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
        when(imageRepository.findByPostOrderByImageOrderAsc(any(CommunityPostEntity.class))).thenReturn(Collections.emptyList());

        // when
        CommunityPostResponse response = communityPostService.getPost(postId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.postId()).isEqualTo(postId);
        assertThat(response.title()).isEqualTo("Test Title");
        assertThat(response.content()).isEqualTo("Test Content");
        assertThat(response.category()).isEqualTo("Test Category");

        verify(imageRepository).findByPostOrderByImageOrderAsc(any(CommunityPostEntity.class));
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
        when(imageRepository.findByPostOrderByImageOrderAsc(any(CommunityPostEntity.class))).thenReturn(Collections.emptyList());

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

    /**
     * 예외 테스트
     */

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

        // 가짜 객체 설정
        given(postRepository.findByPostIdAndIsDeletedFalse(postId))
                .willReturn(Optional.of(post));

        // then
        assertThatThrownBy(() ->
                // 다른 사용자가 게시글 수정
                communityPostService.updatePost(postId, request, otherUserId))
                // CommunityException이 맞는지 확인
                .isInstanceOf(CommunityException.class)
                .hasMessage("본인이 작성한 게시글만 수정/삭제할 수 있습니다")
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COMMUNITY_POST_FORBIDDEN);
    }

    @DisplayName("다른 사용자가 게시글 삭제")
    @Test
    void otherUserDeletePost() {
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

        // 가짜 객체 설정
        given(postRepository.findByPostIdAndIsDeletedFalse(postId))
                .willReturn(Optional.of(post));

        // then
        assertThatThrownBy(() ->
                // 다른 사용자가 게시글 삭제
                communityPostService.deletePost(postId, otherUserId))
                // CommunityException이 맞는지 확인
                .isInstanceOf(CommunityException.class)
                .hasMessage("본인이 작성한 게시글만 수정/삭제할 수 있습니다")
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COMMUNITY_POST_FORBIDDEN);
    }
    
    @DisplayName("존재하지 않는 게시글 조회수 증가")
    @Test
    void countUpNonExistPostView() {
        // Given
        Long nonExistPostId = 999L;
        
        // 가짜 객체 설정
        given(postRepository.findByPostIdAndIsDeletedFalse(nonExistPostId))
                .willReturn(Optional.empty());

        // then
        assertThatThrownBy(() ->
                // 가짜 객체 조회
                communityPostService.getPost(nonExistPostId))
                // CommunityException이 맞는지 확인
                .isInstanceOf(CommunityException.class)
                .hasMessage("해당하는 게시글을 찾을 수 없습니다")
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COMMUNITY_POST_NOT_FOUND);
    }

    @DisplayName("존재하지 않는 게시글 좋아요 증가")
    @Test
    void countUpNonExistPostLike() {
        // Given
        Long nonExistPostId = 999L;

        Long authorId = 100L;

        // 가짜 객체 설정
        given(postRepository.findByPostIdAndIsDeletedFalse(nonExistPostId))
                .willReturn(Optional.empty());

        // when, then
        assertThatThrownBy(() ->
                // 가짜 객체 좋아요
                communityCountService.togglePostLike(nonExistPostId, authorId))
                // CommunityException이 맞는지 확인
                .isInstanceOf(CommunityException.class)
                .hasMessage("해당하는 게시글을 찾을 수 없습니다")
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COMMUNITY_POST_NOT_FOUND);
    }

    @DisplayName("다른 사용자가 댓글 수정")
    @Test
    void otherUserUpdateComment() {
        Long postId = 1L;
        Long commentId = 1L;
        Long authorId = 100L;
        Long otherUserId = 200L;

        User author = User.builder()
                .id(authorId)
                .name("test author")
                .build();

        CommunityPostEntity post = CommunityPostEntity.builder()
                .postId(postId)
                .author(author)
                .build();

        CommunityCommentEntity comment = CommunityCommentEntity.builder()
                .post(post)
                .commentId(commentId)
                .author(author)
                .content("test comment")
                .build();

        CommunityCommentRequest request = new CommunityCommentRequest("update comment", null);

        // 댓글 존재하도록 설정
        given(commentRepository.findById(commentId))
                .willReturn(Optional.of(comment));

        // when, then
        assertThatThrownBy(() ->
                communityCommentService.updateComment(postId, request, otherUserId))

                .isInstanceOf(CommunityException.class)
                .hasMessage("본인이 작성한 댓글만 수정/삭제할 수 있습니다")
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COMMUNITY_COMMENT_FORBIDDEN);
    }

    @DisplayName("다른 사용자가 댓글 삭제")
    @Test
    void otherUserDeleteComment() {
        Long postId = 1L;
        Long commentId = 1L;
        Long authorId = 100L;
        Long otherUserId = 200L;

        User author = User.builder()
                .id(authorId)
                .name("test author")
                .build();

        CommunityPostEntity post = CommunityPostEntity.builder()
                .postId(postId)
                .author(author)
                .build();

        CommunityCommentEntity comment = CommunityCommentEntity.builder()
                .post(post)
                .commentId(commentId)
                .author(author)
                .content("test comment")
                .build();

        // 댓글 존재하도록 설정
        given(commentRepository.findById(commentId))
                .willReturn(Optional.of(comment));

        // when, then
        assertThatThrownBy(() ->
                // 다른유저가 삭제시도
                communityCommentService.deleteComment(commentId, postId, otherUserId))

                .isInstanceOf(CommunityException.class)
                .hasMessage("본인이 작성한 댓글만 수정/삭제할 수 있습니다")
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COMMUNITY_COMMENT_FORBIDDEN);
    }

    @DisplayName("존재하지 않는 게시글에 댓글 작성")
    @Test
    void createCommentOnNonExistPost() {
        // given
        Long nonExistPostId = 999L;
        Long userId = 1L;
        CommunityCommentRequest request = new CommunityCommentRequest("test comment", null);

        User fakeUser = User.builder()
                .id(userId)
                .name("test user")
                .build();

        given(userRepository.findById(userId))
                .willReturn(Optional.of(fakeUser));
        given(postRepository.findByPostIdAndIsDeletedFalse(nonExistPostId))
                .willReturn(Optional.empty());

        // when, then
        assertThatThrownBy(() ->
                communityCommentService.createComment(nonExistPostId, request, userId))
                .isInstanceOf(CommunityException.class)
                .hasMessage("해당하는 게시글을 찾을 수 없습니다")
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COMMUNITY_POST_NOT_FOUND);
    }

    @DisplayName("존재하지 않는 댓글 수정")
    @Test
    void updateNonExistComment() {
        // given
        Long nonExistCommentId = 999L;
        Long userId = 1L;
        CommunityCommentRequest request = new CommunityCommentRequest("updated comment", null);

        given(commentRepository.findById(nonExistCommentId))
                .willReturn(Optional.empty());

        // when, then
        assertThatThrownBy(() ->
                communityCommentService.updateComment(nonExistCommentId, request, userId))
                .isInstanceOf(CommunityException.class)
                .hasMessage("해당하는 댓글을 찾을 수 없습니다")
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COMMUNITY_COMMENT_NOT_FOUND);
    }

    @DisplayName("존재하지 않는 댓글 삭제")
    @Test
    void deleteNonExistComment() {
        // given
        Long nonExistCommentId = 999L;
        Long postId = 1L;
        Long userId = 1L;

        given(commentRepository.findById(nonExistCommentId))
                .willReturn(Optional.empty());

        // when, then
        assertThatThrownBy(() ->
                communityCommentService.deleteComment(nonExistCommentId, postId, userId))
                .isInstanceOf(CommunityException.class)
                .hasMessage("해당하는 댓글을 찾을 수 없습니다")
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COMMUNITY_COMMENT_NOT_FOUND);
    }

    @DisplayName("존재하지 않는 댓글에 좋아요")
    @Test
    void likeNonExistComment() {
        // given
        Long nonExistCommentId = 999L;
        Long userId = 1L;

        given(commentRepository.findById(nonExistCommentId))
                .willReturn(Optional.empty());

        // when, then
        assertThatThrownBy(() ->
                communityCountService.toggleCommentLike(nonExistCommentId, userId))
                .isInstanceOf(CommunityException.class)
                .hasMessage("해당하는 댓글을 찾을 수 없습니다")
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COMMUNITY_COMMENT_NOT_FOUND);
    }
}