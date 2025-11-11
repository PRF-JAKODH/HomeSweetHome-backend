package com.homesweet.homesweetback.domain.community.service;

import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.repository.UserRepository;
import com.homesweet.homesweetback.domain.community.dto.*;
import com.homesweet.homesweetback.domain.community.dto.exception.CommunityException;
import com.homesweet.homesweetback.domain.community.entity.*;
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
    private CommunityPostLikeRepository postLikeRepository;

    @Mock
    private CommunityCommentLikeRepository commentLikeRepository;

    @Mock
    private NotificationSendService notificationSendService;

    @Mock
    private CommunityImageUploader imageUploader;

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
        when(imageRepository.findByPostOrderByImageOrderAsc(any(CommunityPostEntity.class)))
                .thenReturn(Collections.emptyList());

        // when
        Page<CommunityPostResponse> result = communityPostService.getPosts(pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent().get(0).title()).isEqualTo("첫 번째 게시물");
        assertThat(result.getContent().get(1).title()).isEqualTo("두 번째 게시물");

        verify(postRepository).findByIsDeletedFalse(pageable);
    }

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
        when(postRepository.findByPostIdAndIsDeletedFalse(postId)).thenReturn(Optional.of(fakePost));
        when(commentRepository.save(any(CommunityCommentEntity.class))).thenReturn(savedComment);

        // when
        CommunityCommentResponse response = communityCommentService.createComment(postId, request, userId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.content()).isEqualTo(commentContent);
        assertThat(response.authorName()).isEqualTo("User");

        verify(userRepository).findById(userId);
        verify(postRepository).findByPostIdAndIsDeletedFalse(postId);
        verify(commentRepository).save(any(CommunityCommentEntity.class));
    }

    @DisplayName("대댓글 작성 테스트")
    @Test
    void createReplyComment() {
        // given
        Long postId = 1L;
        Long userId = 1L;
        Long parentCommentId = 1L;
        String replyContent = "대댓글입니다.";

        User fakeUser = User.builder().id(userId).name("User").build();
        CommunityPostEntity fakePost = CommunityPostEntity.builder()
                .postId(postId)
                .author(fakeUser)
                .title("Test Title")
                .content("Test Content")
                .category("Test Category")
                .build();

        CommunityCommentEntity parentComment = CommunityCommentEntity.builder()
                .commentId(parentCommentId)
                .post(fakePost)
                .author(fakeUser)
                .content("부모 댓글")
                .build();

        CommunityCommentRequest request = new CommunityCommentRequest(replyContent, parentCommentId);

        CommunityCommentEntity savedReply = CommunityCommentEntity.builder()
                .commentId(2L)
                .post(fakePost)
                .author(fakeUser)
                .content(replyContent)
                .parentCommentId(parentCommentId)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(fakeUser));
        when(postRepository.findByPostIdAndIsDeletedFalse(postId)).thenReturn(Optional.of(fakePost));
        when(commentRepository.findById(parentCommentId)).thenReturn(Optional.of(parentComment));
        when(commentRepository.save(any(CommunityCommentEntity.class))).thenReturn(savedReply);

        // when
        CommunityCommentResponse response = communityCommentService.createComment(postId, request, userId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.content()).isEqualTo(replyContent);
        assertThat(response.parentCommentId()).isEqualTo(parentCommentId);

        verify(commentRepository).findById(parentCommentId);
    }

    @DisplayName("게시글의 댓글 목록 조회 테스트")
    @Test
    void getCommentsByPostId() {
        // given
        Long postId = 1L;
        User fakeUser = User.builder().id(1L).name("User").build();
        CommunityPostEntity fakePost = CommunityPostEntity.builder()
                .postId(postId)
                .author(fakeUser)
                .title("Test Title")
                .content("Test Content")
                .category("Test Category")
                .build();

        CommunityCommentEntity comment1 = CommunityCommentEntity.builder()
                .commentId(1L)
                .post(fakePost)
                .author(fakeUser)
                .content("첫 번째 댓글")
                .build();

        CommunityCommentEntity comment2 = CommunityCommentEntity.builder()
                .commentId(2L)
                .post(fakePost)
                .author(fakeUser)
                .content("두 번째 댓글")
                .build();

        when(commentRepository.findByPost_PostIdAndIsDeletedFalse(postId))
                .thenReturn(Arrays.asList(comment1, comment2));

        // when
        List<CommunityCommentResponse> responses = communityCommentService.getCommentsByPostId(postId);

        // then
        assertThat(responses).isNotNull();
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).content()).isEqualTo("첫 번째 댓글");
        assertThat(responses.get(1).content()).isEqualTo("두 번째 댓글");

        verify(commentRepository).findByPost_PostIdAndIsDeletedFalse(postId);
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
        when(postRepository.findByPostIdAndIsDeletedFalse(postId)).thenReturn(Optional.of(fakePost));

        // when
        communityCommentService.deleteComment(commentId, postId, userId);

        // then
        assertThat(comment.getIsDeleted()).isTrue();
        assertThat(fakePost.getCommentCount()).isEqualTo(0);

        verify(commentRepository).findById(commentId);
        verify(postRepository).findByPostIdAndIsDeletedFalse(postId);
    }

    @DisplayName("게시글 조회수 증가 테스트")
    @Test
    void increaseViewCount() {
        // given
        Long postId = 1L;
        User fakeUser = User.builder().id(1L).name("User").build();
        CommunityPostEntity fakePost = CommunityPostEntity.builder()
                .postId(postId)
                .author(fakeUser)
                .title("Test Title")
                .content("Test Content")
                .category("Test Category")
                .viewCount(0)
                .build();

        when(postRepository.findByPostIdAndIsDeletedFalseWithPessimisticLock(postId)).thenReturn(Optional.of(fakePost));

        // when
        communityCountService.increaseViewCount(postId);

        // then
        assertThat(fakePost.getViewCount()).isEqualTo(1);

        verify(postRepository).findByPostIdAndIsDeletedFalseWithPessimisticLock(postId);
    }

    @DisplayName("게시글 좋아요 추가 테스트")
    @Test
    void addPostLike() {
        // given
        Long postId = 1L;
        Long userId = 1L;

        User fakeUser = User.builder().id(userId).name("User").build();
        CommunityPostEntity fakePost = CommunityPostEntity.builder()
                .postId(postId)
                .author(fakeUser)
                .title("Test Title")
                .content("Test Content")
                .category("Test Category")
                .likeCount(0)
                .build();

        when(postRepository.findByPostIdAndIsDeletedFalseWithPessimisticLock(postId)).thenReturn(Optional.of(fakePost));
        when(userRepository.findById(userId)).thenReturn(Optional.of(fakeUser));
        when(postLikeRepository.findByPostAndUser(fakePost, fakeUser)).thenReturn(Optional.empty());

        // when
        communityCountService.togglePostLike(postId, userId);

        // then
        assertThat(fakePost.getLikeCount()).isEqualTo(1);

        verify(postLikeRepository).save(any(CommunityPostLikeEntity.class));
    }

    @DisplayName("게시글 좋아요 취소 테스트")
    @Test
    void removePostLike() {
        // given
        Long postId = 1L;
        Long userId = 1L;

        User fakeUser = User.builder().id(userId).name("User").build();
        CommunityPostEntity fakePost = CommunityPostEntity.builder()
                .postId(postId)
                .author(fakeUser)
                .title("Test Title")
                .content("Test Content")
                .category("Test Category")
                .likeCount(1)
                .build();

        CommunityPostLikeEntity existingLike = CommunityPostLikeEntity.builder()
                .post(fakePost)
                .user(fakeUser)
                .build();

        when(postRepository.findByPostIdAndIsDeletedFalseWithPessimisticLock(postId)).thenReturn(Optional.of(fakePost));
        when(userRepository.findById(userId)).thenReturn(Optional.of(fakeUser));
        when(postLikeRepository.findByPostAndUser(fakePost, fakeUser)).thenReturn(Optional.of(existingLike));

        // when
        communityCountService.togglePostLike(postId, userId);

        // then
        assertThat(fakePost.getLikeCount()).isEqualTo(0);

        verify(postLikeRepository).delete(existingLike);
    }

    @DisplayName("게시글 좋아요 확인 테스트")
    @Test
    void isPostLiked() {
        // given
        Long postId = 1L;
        Long userId = 1L;

        when(postLikeRepository.existsByPost_PostIdAndUser_Id(postId, userId)).thenReturn(true);

        // when
        boolean result = communityCountService.isPostLiked(postId, userId);

        // then
        assertThat(result).isTrue();

        verify(postLikeRepository).existsByPost_PostIdAndUser_Id(postId, userId);
    }

    @DisplayName("댓글 좋아요 추가 테스트")
    @Test
    void addCommentLike() {
        // given
        Long commentId = 1L;
        Long userId = 1L;

        User fakeUser = User.builder().id(userId).name("User").build();
        CommunityPostEntity fakePost = CommunityPostEntity.builder()
                .postId(1L)
                .author(fakeUser)
                .build();
        CommunityCommentEntity fakeComment = CommunityCommentEntity.builder()
                .commentId(commentId)
                .post(fakePost)
                .author(fakeUser)
                .content("Test Comment")
                .likeCount(0)
                .build();

        when(commentRepository.findByIdWithPessimisticLock(commentId)).thenReturn(Optional.of(fakeComment));
        when(userRepository.findById(userId)).thenReturn(Optional.of(fakeUser));
        when(commentLikeRepository.findByCommentAndUser(fakeComment, fakeUser)).thenReturn(Optional.empty());

        // when
        communityCountService.toggleCommentLike(commentId, userId);

        // then
        assertThat(fakeComment.getLikeCount()).isEqualTo(1);

        verify(commentLikeRepository).save(any(CommunityCommentLikeEntity.class));
    }

    @DisplayName("댓글 좋아요 취소 테스트")
    @Test
    void removeCommentLike() {
        // given
        Long commentId = 1L;
        Long userId = 1L;

        User fakeUser = User.builder().id(userId).name("User").build();
        CommunityPostEntity fakePost = CommunityPostEntity.builder()
                .postId(1L)
                .author(fakeUser)
                .build();
        CommunityCommentEntity fakeComment = CommunityCommentEntity.builder()
                .commentId(commentId)
                .post(fakePost)
                .author(fakeUser)
                .content("Test Comment")
                .likeCount(1)
                .build();

        CommunityCommentLikeEntity existingLike = CommunityCommentLikeEntity.builder()
                .comment(fakeComment)
                .user(fakeUser)
                .build();

        when(commentRepository.findByIdWithPessimisticLock(commentId)).thenReturn(Optional.of(fakeComment));
        when(userRepository.findById(userId)).thenReturn(Optional.of(fakeUser));
        when(commentLikeRepository.findByCommentAndUser(fakeComment, fakeUser))
                .thenReturn(Optional.of(existingLike));

        // when
        communityCountService.toggleCommentLike(commentId, userId);

        // then
        assertThat(fakeComment.getLikeCount()).isEqualTo(0);

        verify(commentLikeRepository).delete(existingLike);
    }

    @DisplayName("댓글 좋아요 확인 테스트")
    @Test
    void isCommentLiked() {
        // given
        Long commentId = 1L;
        Long userId = 1L;

        when(commentLikeRepository.existsByComment_CommentIdAndUser_Id(commentId, userId)).thenReturn(true);

        // when
        boolean result = communityCountService.isCommentLiked(commentId, userId);

        // then
        assertThat(result).isTrue();

        verify(commentLikeRepository).existsByComment_CommentIdAndUser_Id(commentId, userId);
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
        given(postRepository.findByPostIdAndIsDeletedFalseWithPessimisticLock(nonExistPostId))
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
                communityCommentService.updateComment(commentId, request, otherUserId))

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

        given(commentRepository.findByIdWithPessimisticLock(nonExistCommentId))
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