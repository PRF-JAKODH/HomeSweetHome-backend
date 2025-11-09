package com.homesweet.homesweetback.domain.community.service;

import com.homesweet.homesweetback.domain.auth.entity.OAuth2Provider;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.entity.UserRole;
import com.homesweet.homesweetback.domain.auth.repository.UserRepository;
import com.homesweet.homesweetback.domain.community.entity.CommunityPostEntity;
import com.homesweet.homesweetback.domain.community.repository.CommunityPostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

/**
 * Community 동시성 테스트
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
    private UserRepository userRepository;

    private CommunityPostEntity testPost;
    private List<User> testUsers;

    @BeforeEach
    void setUp() {
        // 테스트용 사용자 100명 생성
        testUsers = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
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
                .title("동시성 테스트 게시글")
                .content("100명이 동시에 좋아요 누르면?")
                .category("테스트")
                .build();
        testPost = postRepository.save(testPost);
    }

    @Test
    @DisplayName("동시성 테스트 - 100명이 동시에 좋아요 클릭")
    void concurrentPostLike() throws InterruptedException {
        // given
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when - 100명이 동시에 좋아요 클릭
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

        System.out.println("=== 동시성 테스트 결과 ===");
        System.out.println("성공 횟수: " + successCount.get());
        System.out.println("실패 횟수: " + failCount.get());
        System.out.println("실제 좋아요 수: " + result.getLikeCount());
        System.out.println("예상 좋아요 수: 100");

        // 동시성 제어가 제대로 되면 100이어야 함
        assertThat(result.getLikeCount()).isEqualTo(100);
    }

    @Test
    @DisplayName("동시성 테스트 - 100명이 동시에 조회수 증가")
    void concurrentViewCount() throws InterruptedException {
        // given
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // when - 100명이 동시에 게시글 조회
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

        System.out.println("=== 조회수 동시성 테스트 결과 ===");
        System.out.println("실제 조회수: " + result.getViewCount());
        System.out.println("예상 조회수: 100");

        // 동시성 제어가 안 되면 100보다 작을 수 있음
        assertThat(result.getViewCount()).isEqualTo(100);
    }

    @Test
    @DisplayName("동시성 테스트 - 좋아요 토글 (추가/취소)")
    void concurrentToggleLike() throws InterruptedException {
        // given
        int threadCount = 10;
        int toggleCount = 10; // 각 사용자가 10번씩 토글
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(threadCount * toggleCount);

        // when - 10명이 각각 10번씩 토글 (총 100번)
        for (int i = 0; i < threadCount; i++) {
            final int userIndex = i;
            for (int j = 0; j < toggleCount; j++) {
                executorService.submit(() -> {
                    try {
                        countService.togglePostLike(testPost.getPostId(), testUsers.get(userIndex).getId());
                    } catch (Exception e) {
                        System.err.println("Error: " + e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                });
            }
        }

        latch.await();
        executorService.shutdown();

        // then
        CommunityPostEntity result = postRepository.findById(testPost.getPostId()).orElseThrow();

        System.out.println("=== 좋아요 토글 동시성 테스트 결과 ===");
        System.out.println("실제 좋아요 수: " + result.getLikeCount());
        System.out.println("예상 좋아요 수: 0 (10번씩 토글하면 모두 취소됨)");

        // 각 사용자가 짝수 번 토글하면 최종적으로 좋아요가 없어야 함
        assertThat(result.getLikeCount()).isZero();
    }
}
