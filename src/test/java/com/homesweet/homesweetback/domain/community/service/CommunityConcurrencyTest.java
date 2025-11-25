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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
 * 동시성 테스트
 * - 여러 사용자가 동시에 접근할 때 데이터 정합성 검증
 */
@SpringBootTest
@ActiveProfiles("test")
class CommunityConcurrencyTest {

    @Autowired
    private CommunityCountService countService;

    @Autowired
    private CommunityPostRepository postRepository;

    @Autowired
    private CommunityCommentRepository commentRepository;

    @Autowired
    private UserRepository userRepository;

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
                .content("동시성 테스트 본문?")
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
    }

    @Test
    @DisplayName("동시성 테스트 - 동시에 좋아요 클릭")
    void concurrentPostLike() throws InterruptedException {
        // given
        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when - 동시에 좋아요 클릭
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

        latch.await(); // 모든 스레드가 완료될 때까지 대기
        executorService.shutdown();

        // then
        CommunityPostEntity result = postRepository.findById(testPost.getPostId()).orElseThrow();

        assertThat(result.getLikeCount()).isEqualTo(10);
    }

    @Test
    @DisplayName("동시성 테스트 - 동시에 조회수 증가")
    void concurrentViewCount() throws InterruptedException {
        // given
        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // when - 동시에 게시글 조회
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    countService.increaseViewCount(testPost.getPostId());
                } catch (Exception e) {
                    System.err.println("Error: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then
        CommunityPostEntity result = postRepository.findById(testPost.getPostId()).orElseThrow();

        assertThat(result.getViewCount()).isEqualTo(10);
    }

//    @Test
//    @DisplayName("동시성 테스트 - 좋아요 토글")
//    void concurrentToggleLike() throws InterruptedException {
//        // given
//        int threadCount = 10;
//        int toggleCount = 10;
//        ExecutorService executorService = Executors.newFixedThreadPool(10);
//        CountDownLatch latch = new CountDownLatch(threadCount * toggleCount);
//
//        // when - n명이 n번씩 토글
//        for (int i = 0; i < threadCount; i++) {
//            final int userIndex = i;
//            for (int j = 0; j < toggleCount; j++) {
//                executorService.submit(() -> {
//                    try {
//                        countService.togglePostLike(testPost.getPostId(), testUsers.get(userIndex).getId());
//                    } catch (Exception e) {
//                        System.err.println("Error: " + e.getMessage());
//                    } finally {
//                        latch.countDown();
//                    }
//                });
//            }
//        }
//
//        latch.await();
//        executorService.shutdown();
//
//        // then - 10명이 각각 10번씩 토글하면 모두 0으로 돌아옴
//        CommunityPostEntity result = postRepository.findById(testPost.getPostId()).orElseThrow();
//
//        assertThat(result.getLikeCount()).isZero();
//    }

    @Test
    @DisplayName("동시성 테스트 - 동시에 댓글 좋아요 클릭")
    void concurrentCommentLike() throws InterruptedException {
        // given
        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when - 동시에 댓글 좋아요 클릭
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

        latch.await(); // 모든 스레드가 완료될 때까지 대기
        executorService.shutdown();

        // then
        CommunityCommentEntity result = commentRepository.findById(testComment.getCommentId()).orElseThrow();

        assertThat(result.getLikeCount()).isEqualTo(10);
    }

//    @Test
//    @DisplayName("동시성 테스트 - 댓글 좋아요 토글")
//    void concurrentCommentToggleLike() throws InterruptedException {
//        // given
//        int threadCount = 10;
//        int toggleCount = 10;
//        ExecutorService executorService = Executors.newFixedThreadPool(10);
//        CountDownLatch latch = new CountDownLatch(threadCount * toggleCount);
//
//        // when - n명이 n번씩 토글
//        for (int i = 0; i < threadCount; i++) {
//            final int userIndex = i;
//            for (int j = 0; j < toggleCount; j++) {
//                executorService.submit(() -> {
//                    try {
//                        countService.toggleCommentLike(testComment.getCommentId(), testUsers.get(userIndex).getId());
//                    } catch (Exception e) {
//                        System.err.println("Error: " + e.getMessage());
//                    } finally {
//                        latch.countDown();
//                    }
//                });
//            }
//        }
//
//        latch.await();
//        executorService.shutdown();
//
//        // then - 10명이 각각 10번씩 토글하면 모두 0으로 돌아옴
//        CommunityCommentEntity result = commentRepository.findById(testComment.getCommentId()).orElseThrow();
//
//        assertThat(result.getLikeCount()).isZero();
//    }
}
