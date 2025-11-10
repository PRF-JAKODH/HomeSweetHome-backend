package com.homesweet.homesweetback.domain.notification.domain.payload;

import com.homesweet.homesweetback.domain.notification.domain.NotificationEventType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CommunityNotificationPayload 테스트")
class CommunityNotificationPayloadTest {
    
    @Nested
    @DisplayName("NewCommentPayload 테스트")
    class NewCommentPayloadTest {
        
        @Test
        @DisplayName("정상적인 Payload 생성 및 toMap() 동작")
        void testToMap() {
            // Given
            String userName = "홍길동";
            Long postId = 123L;
            String postTitle = "테스트 게시글";
            
            CommunityNotificationPayload.NewCommentPayload payload = 
                CommunityNotificationPayload.NewCommentPayload.builder()
                    .userName(userName)
                    .postId(postId)
                    .postTitle(postTitle)
                    .build();
            
            // When
            Map<String, Object> result = payload.toMap();
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.get("userName")).isEqualTo(userName);
            assertThat(result.get("postId")).isEqualTo(postId);
            assertThat(result.get("postTitle")).isEqualTo(postTitle);
        }
        
        @Test
        @DisplayName("validate() - 모든 필수 필드가 있을 때 성공")
        void testValidate_Success() {
            // Given
            CommunityNotificationPayload.NewCommentPayload payload = 
                CommunityNotificationPayload.NewCommentPayload.builder()
                    .userName("홍길동")
                    .postId(123L)
                    .postTitle("테스트 게시글")
                    .build();
            
            // When & Then
            payload.validate(NotificationEventType.NEW_COMMENT);
        }
        
        @Test
        @DisplayName("validate() - userName이 null일 때 예외 발생")
        void testValidate_UserNameNull() {
            // Given
            CommunityNotificationPayload.NewCommentPayload payload = 
                CommunityNotificationPayload.NewCommentPayload.builder()
                    .userName(null)
                    .postId(123L)
                    .postTitle("테스트 게시글")
                    .build();
            
            // When & Then
            assertThatThrownBy(() -> payload.validate(NotificationEventType.NEW_COMMENT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("userName is required");
        }
        
        @Test
        @DisplayName("validate() - postId가 null일 때 예외 발생")
        void testValidate_PostIdNull() {
            // Given
            CommunityNotificationPayload.NewCommentPayload payload = 
                CommunityNotificationPayload.NewCommentPayload.builder()
                    .userName("홍길동")
                    .postId(null)
                    .postTitle("테스트 게시글")
                    .build();
            
            // When & Then
            assertThatThrownBy(() -> payload.validate(NotificationEventType.NEW_COMMENT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("postId is required");
        }
        
        @Test
        @DisplayName("validate() - postTitle이 null일 때 예외 발생")
        void testValidate_PostTitleNull() {
            // Given
            CommunityNotificationPayload.NewCommentPayload payload = 
                CommunityNotificationPayload.NewCommentPayload.builder()
                    .userName("홍길동")
                    .postId(123L)
                    .postTitle(null)
                    .build();
            
            // When & Then
            assertThatThrownBy(() -> payload.validate(NotificationEventType.NEW_COMMENT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("postTitle is required");
        }
        
        @Test
        @DisplayName("@SupportsEventType 어노테이션 확인")
        void testSupportsEventTypeAnnotation() {
            // Given
            Class<?> clazz = CommunityNotificationPayload.NewCommentPayload.class;
            
            // When
            SupportsEventType annotation = clazz.getAnnotation(SupportsEventType.class);
            
            // Then
            assertThat(annotation).isNotNull();
            assertThat(annotation.value()).isEqualTo(NotificationEventType.NEW_COMMENT);
        }
    }
    
    @Nested
    @DisplayName("NewLikePayload 테스트")
    class NewLikePayloadTest {
        
        @Test
        @DisplayName("정상적인 Payload 생성 및 toMap() 동작")
        void testToMap() {
            // Given
            String userName = "홍길동";
            Long postId = 123L;
            String postTitle = "테스트 게시글";
            
            CommunityNotificationPayload.NewLikePayload payload = 
                CommunityNotificationPayload.NewLikePayload.builder()
                    .userName(userName)
                    .postId(postId)
                    .postTitle(postTitle)
                    .build();
            
            // When
            Map<String, Object> result = payload.toMap();
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.get("userName")).isEqualTo(userName);
            assertThat(result.get("postId")).isEqualTo(postId.toString());
            assertThat(result.get("postTitle")).isEqualTo(postTitle);
        }
        
        @Test
        @DisplayName("validate() - 모든 필수 필드가 있을 때 성공")
        void testValidate_Success() {
            // Given
            CommunityNotificationPayload.NewLikePayload payload = 
                CommunityNotificationPayload.NewLikePayload.builder()
                    .userName("홍길동")
                    .postId(123L)
                    .postTitle("테스트 게시글")
                    .build();
            
            // When & Then
            payload.validate(NotificationEventType.NEW_LIKE);
        }
        
        @Test
        @DisplayName("validate() - userName이 null일 때 예외 발생")
        void testValidate_UserNameNull() {
            // Given
            CommunityNotificationPayload.NewLikePayload payload = 
                CommunityNotificationPayload.NewLikePayload.builder()
                    .userName(null)
                    .postId(123L)
                    .postTitle("테스트 게시글")
                    .build();
            
            // When & Then
            assertThatThrownBy(() -> payload.validate(NotificationEventType.NEW_LIKE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("userName is required");
        }
        
        @Test
        @DisplayName("validate() - postId가 null일 때 예외 발생")
        void testValidate_PostIdNull() {
            // Given
            CommunityNotificationPayload.NewLikePayload payload = 
                CommunityNotificationPayload.NewLikePayload.builder()
                    .userName("홍길동")
                    .postId(null)
                    .postTitle("테스트 게시글")
                    .build();
            
            // When & Then
            assertThatThrownBy(() -> payload.validate(NotificationEventType.NEW_LIKE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("postId is required");
        }
        
        @Test
        @DisplayName("@SupportsEventType 어노테이션 확인")
        void testSupportsEventTypeAnnotation() {
            // Given
            Class<?> clazz = CommunityNotificationPayload.NewLikePayload.class;
            
            // When
            SupportsEventType annotation = clazz.getAnnotation(SupportsEventType.class);
            
            // Then
            assertThat(annotation).isNotNull();
            assertThat(annotation.value()).isEqualTo(NotificationEventType.NEW_LIKE);
        }
    }
    
    @Nested
    @DisplayName("NewCommentLikePayload 테스트")
    class NewCommentLikePayloadTest {
        
        @Test
        @DisplayName("정상적인 Payload 생성 및 toMap() 동작")
        void testToMap() {
            // Given
            String userName = "홍길동";
            Long postId = 123L;
            String postTitle = "테스트 게시글";
            Long commentId = 456L;
            
            CommunityNotificationPayload.NewCommentLikePayload payload = 
                CommunityNotificationPayload.NewCommentLikePayload.builder()
                    .userName(userName)
                    .postId(postId)
                    .postTitle(postTitle)
                    .commentId(commentId)
                    .build();
            
            // When
            Map<String, Object> result = payload.toMap();
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.get("userName")).isEqualTo(userName);
            assertThat(result.get("postId")).isEqualTo(postId.toString());
            assertThat(result.get("postTitle")).isEqualTo(postTitle);
            assertThat(result.get("commentId")).isEqualTo(commentId.toString());
        }
        
        @Test
        @DisplayName("validate() - 모든 필수 필드가 있을 때 성공")
        void testValidate_Success() {
            // Given
            CommunityNotificationPayload.NewCommentLikePayload payload = 
                CommunityNotificationPayload.NewCommentLikePayload.builder()
                    .userName("홍길동")
                    .postId(123L)
                    .postTitle("테스트 게시글")
                    .commentId(456L)
                    .build();
            
            // When & Then
            payload.validate(NotificationEventType.NEW_COMMENT_LIKE);
        }
        
        @Test
        @DisplayName("validate() - userName이 null일 때 예외 발생")
        void testValidate_UserNameNull() {
            // Given
            CommunityNotificationPayload.NewCommentLikePayload payload = 
                CommunityNotificationPayload.NewCommentLikePayload.builder()
                    .userName(null)
                    .postId(123L)
                    .postTitle("테스트 게시글")
                    .commentId(456L)
                    .build();
            
            // When & Then
            assertThatThrownBy(() -> payload.validate(NotificationEventType.NEW_COMMENT_LIKE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("userName is required");
        }
        
        @Test
        @DisplayName("validate() - postId가 null일 때 예외 발생")
        void testValidate_PostIdNull() {
            // Given
            CommunityNotificationPayload.NewCommentLikePayload payload = 
                CommunityNotificationPayload.NewCommentLikePayload.builder()
                    .userName("홍길동")
                    .postId(null)
                    .postTitle("테스트 게시글")
                    .commentId(456L)
                    .build();
            
            // When & Then
            assertThatThrownBy(() -> payload.validate(NotificationEventType.NEW_COMMENT_LIKE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("postId is required");
        }
        
        @Test
        @DisplayName("validate() - commentId가 null일 때 예외 발생")
        void testValidate_CommentIdNull() {
            // Given
            CommunityNotificationPayload.NewCommentLikePayload payload = 
                CommunityNotificationPayload.NewCommentLikePayload.builder()
                    .userName("홍길동")
                    .postId(123L)
                    .postTitle("테스트 게시글")
                    .commentId(null)
                    .build();
            
            // When & Then
            assertThatThrownBy(() -> payload.validate(NotificationEventType.NEW_COMMENT_LIKE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("commentId is required");
        }
        
        @Test
        @DisplayName("@SupportsEventType 어노테이션 확인")
        void testSupportsEventTypeAnnotation() {
            // Given
            Class<?> clazz = CommunityNotificationPayload.NewCommentLikePayload.class;
            
            // When
            SupportsEventType annotation = clazz.getAnnotation(SupportsEventType.class);
            
            // Then
            assertThat(annotation).isNotNull();
            assertThat(annotation.value()).isEqualTo(NotificationEventType.NEW_COMMENT_LIKE);
        }
    }
}
