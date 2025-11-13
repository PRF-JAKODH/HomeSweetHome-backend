package com.homesweet.homesweetback.domain.notification.domain.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.homesweet.homesweetback.domain.notification.domain.NotificationTemplateType;

@DisplayName("CommunityNotification 테스트")
public class CommunityNotificationTest {

    @Test
    @DisplayName("NewComment 생성 테스트_성공")
    void testCreateNewComment() {
        // Given
        CommunityNotification.NewComment newComment = CommunityNotification.NewComment.builder()
            .userName("홍길동")
            .postId(1L)
            .postTitle("게시글 제목")
            .build();

        // Then
        assertThat(newComment.getUserName()).isEqualTo("홍길동");
        assertThat(newComment.getPostId()).isEqualTo(1L);
        assertThat(newComment.getPostTitle()).isEqualTo("게시글 제목");
        assertThat(newComment.getEventType()).isEqualTo(NotificationTemplateType.NEW_COMMENT);
    }

    @Test
    @DisplayName("NewComment 생성 테스트_실패_userName_null")
    void testCreateNewComment_Failure_UserNameNull() {
        // When & Then
        assertThatThrownBy(() -> CommunityNotification.NewComment.builder()
            .userName(null)
            .postId(1L)
            .postTitle("게시글 제목")
            .build())
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("NewComment 생성 테스트_실패_postId_null")
    void testCreateNewComment_Failure_PostIdNull() {
        // When & Then
        assertThatThrownBy(() -> CommunityNotification.NewComment.builder()
            .userName("홍길동")
            .postId(null)
            .postTitle("게시글 제목")
            .build())
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("NewComment 생성 테스트_실패_postTitle_blank")
    void testCreateNewComment_Failure_PostTitleBlank() {
        // When & Then
        assertThatThrownBy(() -> CommunityNotification.NewComment.builder()
            .userName("홍길동")
            .postId(1L)
            .postTitle("")
            .build())
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("NewLike 생성 테스트_성공")
    void testCreateNewLike() {
        // Given
        CommunityNotification.NewLike newLike = CommunityNotification.NewLike.builder()
            .userName("홍길동")
            .postId(1L)
            .postTitle("게시글 제목")
            .build();

        // Then
        assertThat(newLike.getUserName()).isEqualTo("홍길동");
        assertThat(newLike.getPostId()).isEqualTo(1L);
        assertThat(newLike.getPostTitle()).isEqualTo("게시글 제목");
        assertThat(newLike.getEventType()).isEqualTo(NotificationTemplateType.NEW_LIKE);
    }

    @Test
    @DisplayName("NewLike 생성 테스트_실패_userName_null")
    void testCreateNewLike_Failure_UserNameNull() {
        // When & Then
        assertThatThrownBy(() -> CommunityNotification.NewLike.builder()
            .userName(null)
            .postId(1L)
            .postTitle("게시글 제목")
            .build())
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("NewCommentLike 생성 테스트_성공")
    void testCreateNewCommentLike() {
        // Given
        CommunityNotification.NewCommentLike newCommentLike = CommunityNotification.NewCommentLike.builder()
            .userName("홍길동")
            .postId(1L)
            .postTitle("게시글 제목")
            .commentId(10L)
            .build();

        // Then
        assertThat(newCommentLike.getUserName()).isEqualTo("홍길동");
        assertThat(newCommentLike.getPostId()).isEqualTo(1L);
        assertThat(newCommentLike.getPostTitle()).isEqualTo("게시글 제목");
        assertThat(newCommentLike.getCommentId()).isEqualTo(10L);
        assertThat(newCommentLike.getEventType()).isEqualTo(NotificationTemplateType.NEW_COMMENT_LIKE);
    }

    @Test
    @DisplayName("NewCommentLike 생성 테스트_실패_commentId_null")
    void testCreateNewCommentLike_Failure_CommentIdNull() {
        // When & Then
        assertThatThrownBy(() -> CommunityNotification.NewCommentLike.builder()
            .userName("홍길동")
            .postId(1L)
            .postTitle("게시글 제목")
            .commentId(null)
            .build())
            .isInstanceOf(IllegalArgumentException.class);
    }
}

