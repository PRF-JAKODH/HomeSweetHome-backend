package com.homesweet.homesweetback.domain.notification.domain.notification;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.homesweet.homesweetback.domain.notification.domain.NotificationTemplateType;

import lombok.Builder;
import lombok.Getter;


/**
 * 커뮤니티 관련 알림 클래스
 * 
 * 커뮤니티 관련 알림들을 내부 클래스로 그룹화합니다.
 * 
 * @author dogyungkim
 */
public class CommunityNotification {
    
    /**
     * 새 댓글 알림
     * 
     * 📋 필요한 필드:
     * - userName: String - 댓글 작성자 이름
     * - postId: Long - 게시글 ID
     * - postTitle: String - 게시글 제목
     */
    @Getter
    public static class NewComment implements TemplateNotification {
        private final String userName;
        private final Long postId;
        private final String postTitle;
        
        @JsonIgnore
        private final NotificationTemplateType eventType = NotificationTemplateType.NEW_COMMENT;
        
        @Builder
        public NewComment(String userName, Long postId, String postTitle) {
            if (userName == null || userName.isBlank()) {
                throw new IllegalArgumentException("userName is required for NEW_COMMENT notification");
            }
            if (postId == null) {
                throw new IllegalArgumentException("postId is required for NEW_COMMENT notification");
            }
            if (postTitle == null || postTitle.isBlank()) {
                throw new IllegalArgumentException("postTitle is required for NEW_COMMENT notification");
            }
            this.userName = userName;
            this.postId = postId;
            this.postTitle = postTitle;
        }
        
        @Override
        public NotificationTemplateType getEventType() {
            return eventType;
        }
        
        @Override
        public void validate() {
            // 생성자에서 이미 검증됨
        }
    }
    
    /**
     * 새 좋아요 알림 (게시글)
     * 
     * 📋 필요한 필드:
     * - userName: String - 좋아요 누른 사용자 이름
     * - postId: Long - 게시글 ID
     * - postTitle: String - 게시글 제목
     */
    @Getter
    public static class NewLike implements TemplateNotification {
        private final String userName;
        private final Long postId;
        private final String postTitle;
        
        @JsonIgnore
        private final NotificationTemplateType eventType = NotificationTemplateType.NEW_LIKE;
        
        @Builder
        public NewLike(String userName, Long postId, String postTitle) {
            if (userName == null || userName.isBlank()) {
                throw new IllegalArgumentException("userName is required for NEW_LIKE notification");
            }
            if (postId == null) {
                throw new IllegalArgumentException("postId is required for NEW_LIKE notification");
            }
            if (postTitle == null || postTitle.isBlank()) {
                throw new IllegalArgumentException("postTitle is required for NEW_LIKE notification");
            }
            this.userName = userName;
            this.postId = postId;
            this.postTitle = postTitle;
        }
        
        @Override
        public NotificationTemplateType getEventType() {
            return eventType;
        }
        
        @Override
        public void validate() {
            // 생성자에서 이미 검증됨
        }
    }
    
    /**
     * 새 댓글 좋아요 알림
     * 
     * 📋 필요한 필드:
     * - userName: String - 좋아요 누른 사용자 이름
     * - postId: Long - 게시글 ID
     * - postTitle: String - 게시글 제목
     * - commentId: Long - 댓글 ID
     */
    @Getter
    public static class NewCommentLike implements TemplateNotification {
        private final String userName;
        private final Long postId;
        private final String postTitle;
        private final Long commentId;
        
        @JsonIgnore
        private final NotificationTemplateType eventType = NotificationTemplateType.NEW_COMMENT_LIKE;
        
        @Builder
        public NewCommentLike(String userName, Long postId, String postTitle, Long commentId) {
            if (userName == null || userName.isBlank()) {
                throw new IllegalArgumentException("userName is required for NEW_COMMENT_LIKE notification");
            }
            if (postId == null) {
                throw new IllegalArgumentException("postId is required for NEW_COMMENT_LIKE notification");
            }
            if (postTitle == null || postTitle.isBlank()) {
                throw new IllegalArgumentException("postTitle is required for NEW_COMMENT_LIKE notification");
            }
            if (commentId == null) {
                throw new IllegalArgumentException("commentId is required for NEW_COMMENT_LIKE notification");
            }
            this.userName = userName;
            this.postId = postId;
            this.postTitle = postTitle;
            this.commentId = commentId;
        }
        
        @Override
        public NotificationTemplateType getEventType() {
            return eventType;
        }
        
        @Override
        public void validate() {
            // 생성자에서 이미 검증됨
        }
    }
}
