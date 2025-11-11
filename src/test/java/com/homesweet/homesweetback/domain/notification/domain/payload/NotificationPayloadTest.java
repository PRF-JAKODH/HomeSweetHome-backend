package com.homesweet.homesweetback.domain.notification.domain.payload;

import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.notification.domain.NotificationEventType;
import com.homesweet.homesweetback.domain.notification.exception.NotificationException;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@DisplayName("NotificationPayload 추상 클래스 테스트")
class NotificationPayloadTest {

    /**
     * 방법 1: 테스트용 구체 클래스 생성
     * - 추상 클래스의 공통 로직을 테스트하기 위한 최소한의 구현
     */
    @SupportsEventType(NotificationEventType.NEW_COMMENT)
    static class TestPayload extends NotificationPayload {
        private final String testField;
        private final boolean shouldFailValidation;

        public TestPayload(String testField, boolean shouldFailValidation) {
            this.testField = testField;
            this.shouldFailValidation = shouldFailValidation;
        }

        @Override
        public Map<String, Object> toMap() {
            return Map.of("testField", testField);
        }

        @Override
        protected void validateRequiredFields() {
            if (shouldFailValidation) {
                throw new IllegalArgumentException("테스트 실패");
            }
        }
    }

    /**
     * 어노테이션 없는 테스트용 Payload
     */
    static class NoAnnotationPayload extends NotificationPayload {
        @Override
        public Map<String, Object> toMap() {
            return Map.of();
        }
    }

    @Nested
    @DisplayName("validate() 메서드 테스트")
    class ValidateTest {

        @Test
        @DisplayName("유효한 EventType과 Payload가 일치하면 검증 성공")
        void validate_Success_WhenEventTypeMatches() {
            // given
            TestPayload payload = new TestPayload("test", false);

            // when & then
            assertThatCode(() -> payload.validate(NotificationEventType.NEW_COMMENT))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("EventType과 Payload가 일치하지 않으면 NotificationException 발생")
        void validate_ThrowsException_WhenEventTypeMismatch() {
            // given
            TestPayload payload = new TestPayload("test", false);

            // when & then
            assertThatThrownBy(() -> payload.validate(NotificationEventType.ORDER_COMPLETED))
                    .isInstanceOf(NotificationException.class)
                    .hasMessageContaining(ErrorCode.NOTIFICATION_EVENT_TYPE_MISMATCH.getMessage());
        }

        @Test
        @DisplayName("어노테이션이 없으면 NotificationException 발생")
        void validate_ThrowsException_WhenNoAnnotation() {
            // given
            NoAnnotationPayload payload = new NoAnnotationPayload();

            // when & then
            assertThatThrownBy(() -> payload.validate(NotificationEventType.NEW_COMMENT))
                    .isInstanceOf(NotificationException.class)
                    .hasMessageContaining(ErrorCode.NOTIFICATION_EVENT_TYPE_MISMATCH.getMessage());
        }
    }

    @Nested
    @DisplayName("toMap() 메서드 테스트")
    class ToMapTest {

        @Test
        @DisplayName("Payload를 Map으로 변환")
        void toMap_ReturnsCorrectMap() {
            // given
            TestPayload payload = new TestPayload("testValue", false);

            // when
            Map<String, Object> result = payload.toMap();

            // then
            assertThat(result).containsEntry("testField", "testValue");
        }
    }

    @Nested
    @DisplayName("실제 구현체를 통한 통합 테스트")
    class RealImplementationTest {

        /**
         * 방법 3: 실제 서브클래스를 사용한 테스트
         */
        @Test
        @DisplayName("CommunityNotificationPayload.NewCommentPayload 검증 성공")
        void realPayload_Validation_Success() {
            // given
            CommunityNotificationPayload.NewCommentPayload payload = 
                    CommunityNotificationPayload.NewCommentPayload.builder()
                            .userName("홍길동")
                            .postId(1L)
                            .postTitle("테스트 게시글")
                            .build();

            // when & then
            assertThatCode(() -> payload.validate(NotificationEventType.NEW_COMMENT))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("CommunityNotificationPayload 필수 필드 누락 시 실패")
        void realPayload_Validation_FailsWhenRequiredFieldMissing() {
            // given - userName 누락
            CommunityNotificationPayload.NewCommentPayload payload = 
                    CommunityNotificationPayload.NewCommentPayload.builder()
                            .userName(null)
                            .postId(1L)
                            .postTitle("테스트 게시글")
                            .build();

            // when & then
            assertThatThrownBy(() -> payload.validate(NotificationEventType.NEW_COMMENT))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("userName is required");
        }
    }

    @Nested
    @DisplayName("익명 클래스를 사용한 테스트")
    class AnonymousClassTest {

        /**
         * 방법 2: 익명 클래스 사용
         * - 간단한 테스트 케이스에 유용
         */
        @Test
        @DisplayName("익명 클래스로 추상 클래스 테스트")
        void anonymousClass_Test() {
            // given
            NotificationPayload payload = new NotificationPayload() {
                @Override
                public Map<String, Object> toMap() {
                    return Map.of("anonymous", "test");
                }

                @Override
                protected void validateRequiredFields() {
                    // 검증 통과
                }
            };

            // when
            Map<String, Object> result = payload.toMap();

            // then
            assertThat(result).containsEntry("anonymous", "test");
        }
    }
}
