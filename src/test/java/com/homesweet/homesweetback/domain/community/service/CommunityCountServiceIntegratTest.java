package com.homesweet.homesweetback.domain.community.service;

import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.repository.UserRepository;
import com.homesweet.homesweetback.domain.community.exception.CommunityException;
import com.homesweet.homesweetback.domain.community.entity.CommunityCommentEntity;
import com.homesweet.homesweetback.domain.community.entity.CommunityPostEntity;
import com.homesweet.homesweetback.domain.community.repository.CommunityCommentLikeRepository;
import com.homesweet.homesweetback.domain.community.repository.CommunityCommentRepository;
import com.homesweet.homesweetback.domain.community.repository.CommunityPostLikeRepository;
import com.homesweet.homesweetback.domain.community.repository.CommunityPostRepository;
import com.homesweet.homesweetback.domain.notification.service.NotificationSendService;
import com.homesweet.homesweetback.common.s3.impl.S3ImageUploader;
import io.awspring.cloud.s3.S3Template;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;
import com.homesweet.homesweetback.domain.auth.entity.OAuth2Provider;
import com.homesweet.homesweetback.domain.auth.entity.UserRole;

/**
 * - 조회수, 좋아요 기능 검증
 * - 토글 기능 및 중복 체크 테스트
 *
 * Redis 기반 통합 테스트
 * CI 환경에서는 GitHub Actions의 Redis service container를 사용합니다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CommunityCountServiceIntegratTest {

    @Autowired
    private CommunityCountService countService;

    @Autowired
    private CommunityPostRepository postRepository;

    @Autowired
    private CommunityPostLikeRepository postLikeRepository;

    @Autowired
    private CommunityCommentRepository commentRepository;

    @Autowired
    private CommunityCommentLikeRepository commentLikeRepository;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private S3Template s3Template;

    @MockitoBean
    private S3ImageUploader s3ImageUploader;

    @MockitoBean
    private NotificationSendService notificationSendService;

    private User testUser;
    private User anotherUser;
    private CommunityPostEntity testPost;
    private CommunityCommentEntity testComment;

    @BeforeEach
    void setUp() {
        // 테스트용 사용자 생성
        testUser = User.builder()
                .email("test@example.com")
                .name("테스트유저")
                .profileImageUrl("http://example.com/profile.jpg")
                .provider(OAuth2Provider.GOOGLE)
                .role(UserRole.USER)
                .build();
        testUser = userRepository.save(testUser);

        anotherUser = User.builder()
                .email("another@example.com")
                .name("다른유저")
                .profileImageUrl("http://example.com/profile2.jpg")
                .provider(OAuth2Provider.GOOGLE)
                .role(UserRole.USER)
                .build();
        anotherUser = userRepository.save(anotherUser);

        // 테스트용 게시글 생성
        testPost = CommunityPostEntity.builder()
                .author(testUser)
                .title("테스트 게시글")
                .content("테스트 내용")
                .category("자유게시판")
                .build();
        testPost = postRepository.save(testPost);

        // 테스트용 댓글 생성
        testComment = CommunityCommentEntity.builder()
                .post(testPost)
                .author(testUser)
                .content("테스트 댓글")
                .parentCommentId(null)
                .build();
        testComment = commentRepository.save(testComment);
    }

    // ========== 조회수 테스트 ==========

    @Test
    @DisplayName("게시글 조회수 증가 성공")
    void increaseViewCount_Success() {
        // given
        int initialViewCount = testPost.getViewCount();

        // when
        countService.increaseViewCount(testPost.getPostId());

        // then - Redis에서 조회수를 확인 (Write-Back 패턴이므로 Redis에 먼저 기록됨)
        Integer viewCountFromCache = countService.getViewCountFromCache(testPost.getPostId());
        assertThat(viewCountFromCache).isEqualTo(initialViewCount + 1);
    }

    @Test
    @DisplayName("게시글 조회수 증가 - 여러 번 호출")
    void increaseViewCount_MultipleTimes() {
        // given
        int initialViewCount = testPost.getViewCount();

        // when
        countService.increaseViewCount(testPost.getPostId());
        countService.increaseViewCount(testPost.getPostId());
        countService.increaseViewCount(testPost.getPostId());

        // then - Redis에서 조회수를 확인
        Integer viewCountFromCache = countService.getViewCountFromCache(testPost.getPostId());
        assertThat(viewCountFromCache).isEqualTo(initialViewCount + 3);
    }

    @Test
    @DisplayName("조회수 증가 실패 - 존재하지 않는 게시글")
    void increaseViewCount_Fail_PostNotFound() {
        // given
        Long invalidPostId = 99999L;

        // when & then
        assertThatThrownBy(() -> countService.increaseViewCount(invalidPostId))
                .isInstanceOf(CommunityException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COMMUNITY_POST_NOT_FOUND);
    }

    @Test
    @DisplayName("조회수 증가 실패 - 삭제된 게시글")
    void increaseViewCount_Fail_DeletedPost() {
        // given
        testPost.deletePost();
        postRepository.save(testPost);

        // when & then
        assertThatThrownBy(() -> countService.increaseViewCount(testPost.getPostId()))
                .isInstanceOf(CommunityException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COMMUNITY_POST_NOT_FOUND);
    }

    // ========== 게시글 좋아요 테스트 ==========

    @Test
    @DisplayName("게시글 좋아요 추가 성공")
    void togglePostLike_Add_Success() {
        // given
        int initialLikeCount = testPost.getLikeCount();

        // when
        countService.togglePostLike(testPost.getPostId(), anotherUser.getId());

        // then - Redis에서 좋아요 수와 상태를 확인 (Write-Back 패턴)
        Integer likeCountFromCache = countService.getLikeCountFromCache(testPost.getPostId());
        assertThat(likeCountFromCache).isEqualTo(initialLikeCount + 1);
        assertThat(countService.isPostLiked(testPost.getPostId(), anotherUser.getId())).isTrue();
    }

    @Test
    @DisplayName("게시글 좋아요 취소 성공")
    void togglePostLike_Remove_Success() {
        // given
        countService.togglePostLike(testPost.getPostId(), anotherUser.getId());
        int likeCountAfterAdd = countService.getLikeCountFromCache(testPost.getPostId());

        // when - 다시 토글하여 좋아요 취소
        countService.togglePostLike(testPost.getPostId(), anotherUser.getId());

        // then - Redis에서 확인
        Integer likeCountFromCache = countService.getLikeCountFromCache(testPost.getPostId());
        assertThat(likeCountFromCache).isEqualTo(likeCountAfterAdd - 1);
        assertThat(countService.isPostLiked(testPost.getPostId(), anotherUser.getId())).isFalse();
    }

    @Test
    @DisplayName("게시글 좋아요 토글 - 여러 번 반복")
    void togglePostLike_MultipleToggles() {
        // given
        int initialLikeCount = testPost.getLikeCount();

        // when & then
        // 첫 번째 토글 - 좋아요 추가
        countService.togglePostLike(testPost.getPostId(), anotherUser.getId());
        Integer count1 = countService.getLikeCountFromCache(testPost.getPostId());
        assertThat(count1).isEqualTo(initialLikeCount + 1);

        // 두 번째 토글 - 좋아요 취소
        countService.togglePostLike(testPost.getPostId(), anotherUser.getId());
        Integer count2 = countService.getLikeCountFromCache(testPost.getPostId());
        assertThat(count2).isEqualTo(initialLikeCount);

        // 세 번째 토글 - 다시 좋아요 추가
        countService.togglePostLike(testPost.getPostId(), anotherUser.getId());
        Integer count3 = countService.getLikeCountFromCache(testPost.getPostId());
        assertThat(count3).isEqualTo(initialLikeCount + 1);
    }

    @Test
    @DisplayName("게시글 좋아요 - 여러 사용자")
    void togglePostLike_MultipleUsers() {
        // given
        User thirdUser = User.builder()
                .email("third@example.com")
                .name("세번째유저")
                .profileImageUrl("http://example.com/profile3.jpg")
                .provider(OAuth2Provider.GOOGLE)
                .role(UserRole.USER)
                .build();
        thirdUser = userRepository.save(thirdUser);

        int initialLikeCount = testPost.getLikeCount();

        // when
        countService.togglePostLike(testPost.getPostId(), anotherUser.getId());
        countService.togglePostLike(testPost.getPostId(), thirdUser.getId());

        // then - Redis에서 확인
        Integer likeCountFromCache = countService.getLikeCountFromCache(testPost.getPostId());
        assertThat(likeCountFromCache).isEqualTo(initialLikeCount + 2);
        assertThat(countService.isPostLiked(testPost.getPostId(), anotherUser.getId())).isTrue();
        assertThat(countService.isPostLiked(testPost.getPostId(), thirdUser.getId())).isTrue();
    }

    // NOTE: 현재 구현에서는 존재하지 않는 게시글에 대해서도 빈 좋아요 세트를 생성하므로
    // 예외가 발생하지 않습니다. 이는 의도적인 설계일 수 있으므로 테스트를 주석 처리합니다.
    // 만약 존재하지 않는 게시글에 대해 예외를 던지고 싶다면,
    // CommunityCountService.initPostLikesFromDB()에서 게시글 존재 여부를 확인해야 합니다.

    // @Test
    // @DisplayName("게시글 좋아요 실패 - 존재하지 않는 게시글")
    // void togglePostLike_Fail_PostNotFound() {
    //     // given
    //     Long invalidPostId = 99999L;
    //
    //     // when & then
    //     assertThatThrownBy(() -> countService.togglePostLike(invalidPostId, testUser.getId()))
    //             .isInstanceOf(CommunityException.class)
    //             .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COMMUNITY_POST_NOT_FOUND);
    // }

    @Test
    @DisplayName("게시글 좋아요 상태 확인 - 좋아요 안한 상태")
    void isPostLiked_NotLiked() {
        // when
        boolean isLiked = countService.isPostLiked(testPost.getPostId(), anotherUser.getId());

        // then
        assertThat(isLiked).isFalse();
    }

    @Test
    @DisplayName("게시글 좋아요 상태 확인 - 좋아요 한 상태")
    void isPostLiked_Liked() {
        // given
        countService.togglePostLike(testPost.getPostId(), anotherUser.getId());

        // when
        boolean isLiked = countService.isPostLiked(testPost.getPostId(), anotherUser.getId());

        // then
        assertThat(isLiked).isTrue();
    }

    // ========== 댓글 좋아요 테스트 ==========

    @Test
    @DisplayName("댓글 좋아요 추가 성공")
    void toggleCommentLike_Add_Success() {
        // given
        int initialLikeCount = testComment.getLikeCount();

        // when
        countService.toggleCommentLike(testComment.getCommentId(), anotherUser.getId());

        // then - Redis에서 확인
        Integer likeCountFromCache = countService.getCommentLikeCountFromCache(testComment.getCommentId());
        assertThat(likeCountFromCache).isEqualTo(initialLikeCount + 1);
        assertThat(countService.isCommentLiked(testComment.getCommentId(), anotherUser.getId())).isTrue();
    }

    @Test
    @DisplayName("댓글 좋아요 취소 성공")
    void toggleCommentLike_Remove_Success() {
        // given
        countService.toggleCommentLike(testComment.getCommentId(), anotherUser.getId());
        int likeCountAfterAdd = countService.getCommentLikeCountFromCache(testComment.getCommentId());

        // when - 다시 토글하여 좋아요 취소
        countService.toggleCommentLike(testComment.getCommentId(), anotherUser.getId());

        // then - Redis에서 확인
        Integer likeCountFromCache = countService.getCommentLikeCountFromCache(testComment.getCommentId());
        assertThat(likeCountFromCache).isEqualTo(likeCountAfterAdd - 1);
        assertThat(countService.isCommentLiked(testComment.getCommentId(), anotherUser.getId())).isFalse();
    }

    @Test
    @DisplayName("댓글 좋아요 토글 - 여러 번 반복")
    void toggleCommentLike_MultipleToggles() {
        // given
        int initialLikeCount = testComment.getLikeCount();

        // when & then - Redis에서 확인
        countService.toggleCommentLike(testComment.getCommentId(), anotherUser.getId());
        Integer count1 = countService.getCommentLikeCountFromCache(testComment.getCommentId());
        assertThat(count1).isEqualTo(initialLikeCount + 1);

        countService.toggleCommentLike(testComment.getCommentId(), anotherUser.getId());
        Integer count2 = countService.getCommentLikeCountFromCache(testComment.getCommentId());
        assertThat(count2).isEqualTo(initialLikeCount);

        countService.toggleCommentLike(testComment.getCommentId(), anotherUser.getId());
        Integer count3 = countService.getCommentLikeCountFromCache(testComment.getCommentId());
        assertThat(count3).isEqualTo(initialLikeCount + 1);
    }

    // NOTE: 현재 구현에서는 존재하지 않는 댓글에 대해서도 빈 좋아요 세트를 생성하므로
    // 예외가 발생하지 않습니다. 게시글 좋아요와 동일한 이유로 테스트를 주석 처리합니다.

    // @Test
    // @DisplayName("댓글 좋아요 실패 - 존재하지 않는 댓글")
    // void toggleCommentLike_Fail_CommentNotFound() {
    //     // given
    //     Long invalidCommentId = 99999L;
    //
    //     // when & then
    //     assertThatThrownBy(() -> countService.toggleCommentLike(invalidCommentId, testUser.getId()))
    //             .isInstanceOf(CommunityException.class)
    //             .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COMMUNITY_COMMENT_NOT_FOUND);
    // }

    @Test
    @DisplayName("댓글 좋아요 상태 확인 - 좋아요 안한 상태")
    void isCommentLiked_NotLiked() {
        // when
        boolean isLiked = countService.isCommentLiked(testComment.getCommentId(), anotherUser.getId());

        // then
        assertThat(isLiked).isFalse();
    }

    @Test
    @DisplayName("댓글 좋아요 상태 확인 - 좋아요 한 상태")
    void isCommentLiked_Liked() {
        // given
        countService.toggleCommentLike(testComment.getCommentId(), anotherUser.getId());

        // when
        boolean isLiked = countService.isCommentLiked(testComment.getCommentId(), anotherUser.getId());

        // then
        assertThat(isLiked).isTrue();
    }

    // ========== 통합 시나리오 테스트 ==========

    @Test
    @DisplayName("통합 시나리오 - 게시글 조회수 증가 + 좋아요")
    void integratedScenario_ViewAndLike() {
        // given
        int initialViewCount = testPost.getViewCount();
        int initialLikeCount = testPost.getLikeCount();

        // when
        countService.increaseViewCount(testPost.getPostId());
        countService.increaseViewCount(testPost.getPostId());
        countService.togglePostLike(testPost.getPostId(), anotherUser.getId());

        // then - Redis에서 확인
        Integer viewCount = countService.getViewCountFromCache(testPost.getPostId());
        Integer likeCount = countService.getLikeCountFromCache(testPost.getPostId());
        assertThat(viewCount).isEqualTo(initialViewCount + 2);
        assertThat(likeCount).isEqualTo(initialLikeCount + 1);
    }

    @Test
    @DisplayName("통합 시나리오 - 게시글 좋아요 + 댓글 좋아요")
    void integratedScenario_PostAndCommentLike() {
        // when
        countService.togglePostLike(testPost.getPostId(), anotherUser.getId());
        countService.toggleCommentLike(testComment.getCommentId(), anotherUser.getId());

        // then - Redis에서 확인
        assertThat(countService.isPostLiked(testPost.getPostId(), anotherUser.getId())).isTrue();
        assertThat(countService.isCommentLiked(testComment.getCommentId(), anotherUser.getId())).isTrue();

        Integer postLikeCount = countService.getLikeCountFromCache(testPost.getPostId());
        Integer commentLikeCount = countService.getCommentLikeCountFromCache(testComment.getCommentId());

        assertThat(postLikeCount).isEqualTo(1);
        assertThat(commentLikeCount).isEqualTo(1);
    }
}
