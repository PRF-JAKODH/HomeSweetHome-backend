package com.homesweet.homesweetback.domain.notification.domain.payload;

import com.homesweet.homesweetback.domain.notification.domain.NotificationEventType;

import lombok.Builder;

import java.util.Map;

/**
 * 어노테이션 기반 커뮤니티 관련 알림 Payload 클래스
 * 
 * @SupportsEventType 어노테이션을 사용하여 각 Payload가 지원하는 EventType을 명시합니다.
 * 
 * @author dogyungkim
 */
public class CommunityNotificationPayload {
    
    /**
     * 새 댓글 알림 Payload
     * 
     * 📋 필요한 contextData:
     * - userName: String - 댓글 작성자 이름
     * - postId: String - 게시글 ID
     * - postTitle: String - 게시글 제목
     */
    @SupportsEventType(NotificationEventType.NEW_COMMENT)
    @Builder
    public static class NewCommentPayload extends NotificationPayload {
        private String userName;
        private Long postId;
        private String postTitle;
        
        @Override
        public Map<String, Object> toMap() {
            return Map.of(
                "userName", userName != null ? userName : "",
                "postId", postId != null ? postId : "",
                "postTitle", postTitle != null ? postTitle : ""
            );
        }
        
        @Override
        protected void validateRequiredFields() {
            if (userName == null || userName.isBlank()) {
                throw new IllegalArgumentException("userName is required for NEW_COMMENT notification");
            }
            if (postId == null) {
                throw new IllegalArgumentException("postId is required for NEW_COMMENT notification");
            }
            if (postTitle == null || postTitle.isBlank()) {
                throw new IllegalArgumentException("postTitle is required for NEW_COMMENT notification");
            }
        }
    }
    
    /**
     * 새 좋아요 알림 Payload (게시글)
     * 
     * 📋 필요한 contextData:
     * - userName: String - 좋아요 누른 사용자 이름
     * - postId: String - 게시글 ID
     * - postTitle: String - 게시글 제목
     */
    @SupportsEventType(NotificationEventType.NEW_LIKE)
    @Builder
    public static class NewLikePayload extends NotificationPayload {
        private String userName;
        private Long postId;
        private String postTitle;
        
        @Override
        public Map<String, Object> toMap() {
            return Map.of(
                "userName", userName != null ? userName : "",
                "postId", postId != null ? postId.toString() : "",
                "postTitle", postTitle != null ? postTitle : ""
            );
        }
        
        @Override
        protected void validateRequiredFields() {
            if (userName == null || userName.isBlank()) {
                throw new IllegalArgumentException("userName is required for NEW_LIKE notification");
            }
            if (postId == null) {
                throw new IllegalArgumentException("postId is required for NEW_LIKE notification");
            }
            if (postTitle == null || postTitle.isBlank()) {
                throw new IllegalArgumentException("postTitle is required for NEW_LIKE notification");
            }
        }
    }
    
    /**
     * 새 댓글 좋아요 알림 Payload
     * 
     * 📋 필요한 contextData:
     * - userName: String - 좋아요 누른 사용자 이름
     * - postId: String - 게시글 ID
     * - postTitle: String - 게시글 제목
     * - commentId: String - 댓글 ID
     */
    @SupportsEventType(NotificationEventType.NEW_COMMENT_LIKE)
    @Builder
    public static class NewCommentLikePayload extends NotificationPayload {
        private String userName;
        private Long postId;
        private String postTitle;
        private Long commentId;
        
        @Override
        public Map<String, Object> toMap() {
            return Map.of(
                "userName", userName != null ? userName : "",
                "postId", postId != null ? postId.toString() : "",
                "postTitle", postTitle != null ? postTitle : "",
                "commentId", commentId != null ? commentId.toString() : ""
            );
        }
        
        @Override
        protected void validateRequiredFields() {
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
        }
    }
}

