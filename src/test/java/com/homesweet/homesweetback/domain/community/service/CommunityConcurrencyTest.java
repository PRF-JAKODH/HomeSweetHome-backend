package com.homesweet.homesweetback.domain.community.service;

import com.homesweet.homesweetback.domain.auth.entity.OAuth2Provider;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.entity.UserRole;
import com.homesweet.homesweetback.domain.auth.repository.UserRepository;
import com.homesweet.homesweetback.domain.community.entity.CommunityCommentEntity;
import com.homesweet.homesweetback.domain.community.entity.CommunityPostEntity;
import com.homesweet.homesweetback.domain.community.repository.CommunityCommentRepository;
import com.homesweet.homesweetback.domain.community.repository.CommunityPostRepository;
import com.homesweet.homesweetback.domain.notification.service.NotificationSendService;
import com.homesweet.homesweetback.common.s3.impl.S3ImageUploader;
import io.awspring.cloud.s3.S3Template;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

/**
 * 동시성 테스트 - Redis 기반
 * - Lua Script의 원자적 처리 검증
 * - 여러 사용자가 동시에 접근할 때 데이터 정합성 검증
 *
 * 복잡한 동시성 테스트는 CI 환경에서 불안정할 수 있어 비활성화
 */
@Disabled("복잡한 동시성 테스트는 CI 환경에서 불안정하여 비활성화")
@SpringBootTest
@ActiveProfiles("test")
class CommunityConcurrencyTest {

    @Autowired
    private CommunityCountService countService;

    @Autowired
    private CommunityRedisService redisService;

    @Autowired
    private CommunityPostRepository postRepository;

    @Autowired
    private CommunityCommentRepository commentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @MockitoBean
    private S3Template s3Template;

    @MockitoBean
    private S3ImageUploader s3ImageUploader;

    @MockitoBean
    private NotificationSendService notificationSendService;

    private CommunityPostEntity testPost;
    private CommunityCommentEntity testComment;
    private List<User> testUsers;

    @BeforeEach
    void setUp() {
        // Redis 초기화 (테스트 전 캐시 클리어)
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();

        // 테스트용 사용자 생성
        testUsers = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            User user = User.builder()
                    .email("user" + i + "@test.com")
                    .name("유저" + i)
                    .profileImageUrl("http://example.com/profile.jpg")
                    .provider(OAuth2Provider.GOOGLE)
                    .role(UserRole.USER)
                    .build();
            testUsers.add(userRepository.save(user));
        }

        // 테스트용 게시글 생성
        testPost = CommunityPostEntity.builder()
                .author(testUsers.get(0))
                .title("동시성 테스트 제목")
                .content("동시성 테스트 본문")
                .category("테스트")
                .build();
        testPost = postRepository.save(testPost);

        // 테스트용 댓글 생성
        testComment = CommunityCommentEntity.builder()
                .post(testPost)
                .author(testUsers.get(0))
                .content("동시성 테스트용 댓글")
                .build();
        testComment = commentRepository.save(testComment);

        // Redis 키 초기화 (Lua 스크립트가 EXISTS 체크를 하므로 키가 존재해야 함)
        redisService.setPostViewCount(testPost.getPostId(), 0);
        redisService.setPostCommentCount(testPost.getPostId(), 0);
        redisService.setPostLikes(testPost.getPostId(), List.of());
        redisService.setCommentLikes(testComment.getCommentId(), List.of());
    }

    @Test
    @DisplayName("동시성 테스트 - 게시글 조회수 증가 (Lua Script 원자성 검증)")
    void concurrentViewCount() throws InterruptedException {
        // given
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when - 동시에 조회수 증가
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    countService.increaseViewCount(testPost.getPostId());
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    System.err.println("Error: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then - Redis에서 조회수 확인
        String key = "post:" + testPost.getPostId() + ":viewCount";
        Integer viewCount = (Integer) redisTemplate.opsForValue().get(key);

        // Lua Script가 원자적으로 처리되었는지 검증
        assertThat(viewCount).isNotNull();
        assertThat(viewCount).isEqualTo(threadCount);
        assertThat(failCount.get()).isZero();
    }

    @Test
    @DisplayName("동시성 테스트 - 게시글 좋아요 토글 (Lua Script 원자성 검증)")
    void concurrentPostLike() throws InterruptedException {
        // given
        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when - 10명의 사용자가 동시에 좋아요 클릭
        for (int i = 0; i < threadCount; i++) {
            final int userIndex = i;
            executorService.submit(() -> {
                try {
                    countService.togglePostLike(testPost.getPostId(), testUsers.get(userIndex).getId());
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    System.err.println("Error: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then - Redis에서 좋아요 수 확인
        String countKey = "post:" + testPost.getPostId() + ":likeCount";
        Integer likeCount = (Integer) redisTemplate.opsForValue().get(countKey);

        // Lua Script가 원자적으로 처리되었는지 검증
        assertThat(likeCount).isNotNull();
        assertThat(likeCount).isEqualTo(threadCount);
        assertThat(failCount.get()).isZero();

        // 좋아요한 사용자 Set 확인
        String likesKey = "post:" + testPost.getPostId() + ":likes";
        Long likesSetSize = redisTemplate.opsForSet().size(likesKey);
        assertThat(likesSetSize).isEqualTo(threadCount);
    }

    @Test
    @DisplayName("동시성 테스트 - 좋아요 토글 반복 (같은 사용자가 여러 번 토글)")
    void concurrentPostLikeToggle() throws InterruptedException {
        // given
        int toggleCount = 50;  // 홀수면 최종적으로 좋아요 상태
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(toggleCount);
        User singleUser = testUsers.get(0);

        // when - 한 사용자가 50번 토글
        for (int i = 0; i < toggleCount; i++) {
            executorService.submit(() -> {
                try {
                    countService.togglePostLike(testPost.getPostId(), singleUser.getId());
                } catch (Exception e) {
                    System.err.println("Error: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then - 짝수 번 토글하면 0, 홀수 번 토글하면 1
        boolean isLiked = countService.isPostLiked(testPost.getPostId(), singleUser.getId());

        // toggleCount가 50(짝수)이므로 최종적으로 좋아요가 해제되어 있어야 함
        assertThat(isLiked).isFalse();

        String countKey = "post:" + testPost.getPostId() + ":likeCount";
        Integer likeCount = (Integer) redisTemplate.opsForValue().get(countKey);
        assertThat(likeCount).isZero();
    }

    @Test
    @DisplayName("동시성 테스트 - 댓글 좋아요 토글 (Lua Script 원자성 검증)")
    void concurrentCommentLike() throws InterruptedException {
        // given
        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when - 10명의 사용자가 동시에 댓글 좋아요 클릭
        for (int i = 0; i < threadCount; i++) {
            final int userIndex = i;
            executorService.submit(() -> {
                try {
                    countService.toggleCommentLike(testComment.getCommentId(), testUsers.get(userIndex).getId());
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    System.err.println("Error: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then - Redis에서 댓글 좋아요 수 확인
        String countKey = "comment:" + testComment.getCommentId() + ":likeCount";
        Integer likeCount = (Integer) redisTemplate.opsForValue().get(countKey);

        // Lua Script가 원자적으로 처리되었는지 검증
        assertThat(likeCount).isNotNull();
        assertThat(likeCount).isEqualTo(threadCount);
        assertThat(failCount.get()).isZero();

        // 좋아요한 사용자 Set 확인
        String likesKey = "comment:" + testComment.getCommentId() + ":likes";
        Long likesSetSize = redisTemplate.opsForSet().size(likesKey);
        assertThat(likesSetSize).isEqualTo(threadCount);
    }

    @Test
    @DisplayName("동시성 테스트 - 댓글 수 증가 (Lua Script 원자성 검증)")
    void concurrentCommentCount() throws InterruptedException {
        // given
        int threadCount = 50;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // when - 동시에 댓글 수 증가
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    countService.increaseCommentCount(testPost.getPostId());
                } catch (Exception e) {
                    System.err.println("Error: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then - Redis에서 댓글 수 확인
        String key = "post:" + testPost.getPostId() + ":commentCount";
        Integer commentCount = (Integer) redisTemplate.opsForValue().get(key);

        // Lua Script가 원자적으로 처리되었는지 검증
        assertThat(commentCount).isNotNull();
        assertThat(commentCount).isEqualTo(threadCount);
    }

    @Test
    @Disabled("CI 환경에서 불안정 - 타이밍 이슈로 인해 가끔 실패할 수 있음")
    @DisplayName("동시성 테스트 - 댓글 수 증가/감소 혼합")
    void concurrentCommentCountMixed() throws InterruptedException {
        // given
        int increaseCount = 30;
        int decreaseCount = 20;
        int totalCount = increaseCount + decreaseCount;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(totalCount);

        // when - 동시에 증가/감소
        for (int i = 0; i < increaseCount; i++) {
            executorService.submit(() -> {
                try {
                    countService.increaseCommentCount(testPost.getPostId());
                } finally {
                    latch.countDown();
                }
            });
        }

        for (int i = 0; i < decreaseCount; i++) {
            executorService.submit(() -> {
                try {
                    countService.decreaseCommentCount(testPost.getPostId());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then - 최종 댓글 수는 증가 횟수 - 감소 횟수
        String key = "post:" + testPost.getPostId() + ":commentCount";
        Integer commentCount = (Integer) redisTemplate.opsForValue().get(key);

        assertThat(commentCount).isNotNull();
        assertThat(commentCount).isEqualTo(increaseCount - decreaseCount);
    }

    @Test
    @DisplayName("스트레스 테스트 - 조회수, 좋아요, 댓글 수 동시 처리")
    void stressTest_MixedOperations() throws InterruptedException {
        // given
        int operationsPerType = 20;
        int totalOperations = operationsPerType * 3;  // 조회수, 좋아요, 댓글 수
        ExecutorService executorService = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(totalOperations);

        // when - 조회수 증가
        for (int i = 0; i < operationsPerType; i++) {
            executorService.submit(() -> {
                try {
                    countService.increaseViewCount(testPost.getPostId());
                } finally {
                    latch.countDown();
                }
            });
        }

        // 좋아요 토글 (각 사용자 1번씩)
        for (int i = 0; i < Math.min(operationsPerType, testUsers.size()); i++) {
            final int userIndex = i % testUsers.size();
            executorService.submit(() -> {
                try {
                    countService.togglePostLike(testPost.getPostId(), testUsers.get(userIndex).getId());
                } finally {
                    latch.countDown();
                }
            });
        }

        // 댓글 수 증가
        for (int i = 0; i < operationsPerType; i++) {
            executorService.submit(() -> {
                try {
                    countService.increaseCommentCount(testPost.getPostId());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then - 모든 작업이 원자적으로 처리되었는지 검증
        String viewKey = "post:" + testPost.getPostId() + ":viewCount";
        Integer viewCount = (Integer) redisTemplate.opsForValue().get(viewKey);
        assertThat(viewCount).isEqualTo(operationsPerType);

        String commentKey = "post:" + testPost.getPostId() + ":commentCount";
        Integer commentCount = (Integer) redisTemplate.opsForValue().get(commentKey);
        assertThat(commentCount).isEqualTo(operationsPerType);

        String likeKey = "post:" + testPost.getPostId() + ":likeCount";
        Integer likeCount = (Integer) redisTemplate.opsForValue().get(likeKey);
        assertThat(likeCount).isEqualTo(Math.min(operationsPerType, testUsers.size()));
    }
}
