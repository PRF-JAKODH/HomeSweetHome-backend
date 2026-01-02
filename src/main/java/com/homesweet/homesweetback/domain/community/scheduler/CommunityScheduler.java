package com.homesweet.homesweetback.domain.community.scheduler;

import com.homesweet.homesweetback.domain.community.repository.CommunityCommentLikeRepository;
import com.homesweet.homesweetback.domain.community.repository.CommunityCommentRepository;
import com.homesweet.homesweetback.domain.community.repository.CommunityPostLikeRepository;
import com.homesweet.homesweetback.domain.community.repository.CommunityPostRepository;
import com.homesweet.homesweetback.domain.community.service.CommunityCountService;
import com.homesweet.homesweetback.domain.community.service.CommunityRedisService;
import com.homesweet.homesweetback.domain.community.service.CommunityRedisService.LikeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * [커뮤니티 스케줄러 - Redis에서 DB로 데이터 동기화]
 *
 * [스케줄러란?]
 * 정해진 시간마다 자동으로 실행되는 작업을 담당하는 클래스야.
 * 예를 들면 "매일 새벽 3시에 오래된 로그 삭제" 같은 작업.
 *
 * [이 클래스가 하는 일]
 * 1. Redis(빠른 임시 저장소)에 쌓인 조회수, 좋아요 데이터를 DB에 저장
 * 2. 서버 시작할 때 인기 게시글 캐시 미리 준비 (워밍업)
 *
 * [왜 이렇게 해?]
 * - Redis는 빠르지만 서버 재시작하면 데이터가 사라질 수 있어
 * - 매번 DB에 저장하면 느리니까, 모아서 한 번에 저장하는 게 효율적
 */
@Component // 스프링이 자동으로 이 클래스를 관리하게 등록
@Slf4j // log.info(), log.error() 같은 로그 기능 사용 가능
@RequiredArgsConstructor // 아래 final 변수들을 자동으로 생성자 주입
public class CommunityScheduler {

    // Redis에서 데이터 읽고 쓰는 서비스
    private final CommunityRedisService redisService;
    // 게시글 DB 접근용
    private final CommunityPostRepository communityPostRepository;
    // 게시글 좋아요 DB 접근용
    private final CommunityPostLikeRepository postLikeRepository;
    // 댓글 좋아요 DB 접근용
    private final CommunityCommentLikeRepository commentLikeRepository;
    // 댓글 DB 접근용
    private final CommunityCommentRepository commentRepository;
    // 트랜잭션 수동 관리용 (여러 DB 작업을 하나로 묶을 때 사용)
    private final TransactionTemplate transactionTemplate;
    // 조회수, 좋아요, 댓글 수 관리 서비스
    private final CommunityCountService communityCountService;

    /**
     * [캐시 워밍업 - 인기 게시글 미리 준비]
     *
     * [실행 시점]
     * - 서버 시작 5초 후 첫 실행
     * - 이후 1시간마다 반복 실행
     *
     * [하는 일]
     * 최신 게시글 100개의 좋아요 정보를 DB에서 Redis로 미리 올려놓아.
     * 이렇게 하면 사용자가 게시글 볼 때 DB까지 안 가도 되니까 훨씬 빨라!
     *
     * [워밍업이란?]
     * 자동차 시동 걸고 바로 출발 안 하고 예열하는 것처럼,
     * 서버도 시작하자마자 바로 서비스하기보다 캐시를 미리 준비해두는 거야.
     */
    @Scheduled(initialDelay = 5000, fixedDelayString = "${community.scheduler.warmup-delay:3600000}")
    public void warmupPopularPostsCache() {
        log.info("Starting cache warmup for popular posts...");

        try {
            // 최신 게시글 100개를 DB에서 가져와 (삭제 안 된 것만)
            var recentPosts = communityPostRepository.findByIsDeletedFalse(
                    PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "postId")));

            int warmupCount = 0; // 워밍업 처리된 게시글 수

            // 각 게시글을 돌면서 Redis에 캐시 준비
            for (var post : recentPosts.getContent()) {
                Long postId = post.getPostId();

                // Redis에 이 게시글의 좋아요 정보가 없으면
                if (!redisService.hasPostLikeKey(postId)) {
                    // DB에서 좋아요 누른 사람 목록을 가져와서
                    List<Long> userIds = postLikeRepository.findUserIdsByPostId(postId);
                    // Redis에 저장해둬 (나중에 빠르게 조회하려고)
                    redisService.setPostLikes(postId, userIds);
                    warmupCount++;
                }

                // 조회수, 좋아요수, 댓글수도 미리 캐시에 로드
                communityCountService.getPostCounts(postId);
            }

            log.info("Cache warmup completed - posts: {}, likes warmed: {}",
                    recentPosts.getContent().size(), warmupCount);

        } catch (Exception e) {
            // 워밍업 실패해도 서버는 계속 돌아가야 하니까 에러만 로그로 남김
            log.error("Failed to warmup cache", e);
        }
    }

    /**
     * [조회수 동기화 - Redis에 쌓인 조회수를 DB에 저장]
     *
     * [실행 시점]
     * 약 2분마다 실행
     *
     * [하는 일]
     * 사용자가 게시글을 볼 때마다 조회수가 Redis에 +1씩 쌓여.
     * 이걸 모아서 DB에 한 번에 저장하는 거야.
     *
     * [왜 바로 DB에 안 저장해?]
     * - DB는 무거워서 매번 저장하면 느려
     * - Redis에 쌓아뒀다가 한 번에 저장하면 효율적!
     */
    @Scheduled(initialDelayString = "${community.scheduler.view-sync-delay:100000}", fixedDelayString = "${community.scheduler.view-sync-delay:110000}")
    public void syncViewCounts() {
        // Redis에서 전체 조회수 데이터 수집 (게시글ID -> 조회수)
        Map<Long, Integer> viewCounts = redisService.scanAndCollectViewCounts();

        // 동기화할 게 없으면 그냥 끝
        if (viewCounts.isEmpty()) {
            return;
        }

        // DB에 저장하고, 완료된 Redis 데이터는 삭제
        syncCountsToDb(viewCounts, "view",
                (postId, count) -> communityPostRepository.updateViewCount(postId, count),
                redisService::deletePostViewKey);

        log.info("View count sync completed - processed: {}", viewCounts.size());
    }

    /**
     * [댓글수 동기화 - Redis에 쌓인 댓글수를 DB에 저장]
     *
     * [실행 시점]
     * 약 3~4분마다 실행
     *
     * [하는 일]
     * 댓글이 달리거나 삭제될 때 댓글수 변화가 Redis에 기록돼.
     * 이걸 DB에 반영하는 작업이야.
     */
    @Scheduled(initialDelayString = "${community.scheduler.comment-sync-delay:200000}", fixedDelayString = "${community.scheduler.comment-sync-delay:210000}")
    public void syncCommentCounts() {
        Map<Long, Integer> commentCounts = redisService.scanAndCollectCommentCounts();

        if (commentCounts.isEmpty()) {
            return;
        }

        syncCountsToDb(commentCounts, "comment",
                (postId, count) -> communityPostRepository.setCommentCount(postId, count),
                redisService::deletePostCommentCountKey);

        log.info("Comment count sync completed - processed: {}", commentCounts.size());
    }

    /**
     * [게시글 좋아요 이벤트 동기화 - 좋아요 추가/취소를 DB에 저장]
     *
     * [실행 시점]
     * 약 5분마다 실행
     *
     * [하는 일]
     * 사용자가 좋아요를 누르거나 취소하면 Redis 큐에 기록돼.
     * 이걸 DB에 실제로 반영하는 작업이야.
     *
     * [왜 이렇게 해?]
     * - 좋아요는 자주 눌리니까 매번 DB에 저장하면 부하가 커
     * - Redis에 일단 기록하고 나중에 한꺼번에 DB에 저장하면 효율적!
     */
    @Scheduled(fixedDelayString = "${community.scheduler.like-sync-delay:300000}")
    public void syncPostLikeEvents() {
        // Redis 큐에서 좋아요 이벤트 최대 1000개 가져와
        List<LikeEvent> events = redisService.pollPostLikeEvents(1000);

        if (events.isEmpty()) {
            return;
        }

        // 이벤트들을 DB에 반영 (추가면 INSERT, 취소면 DELETE)
        int[] counts = syncLikeEvents(events,
                postLikeRepository::insertPostLike,
                postLikeRepository::deleteByPostIdAndUserId);

        // 처리 완료된 이벤트는 Redis에서 제거
        redisService.trimPostLikeEvents(events.size());

        log.info("Post like events synced - total: {}, added: {}, removed: {}",
                events.size(), counts[0], counts[1]);
    }

    /**
     * [게시글 좋아요 개수 동기화 - Redis의 좋아요 수를 DB에 저장]
     *
     * [실행 시점]
     * 약 5분마다 실행 (위 이벤트 동기화 후)
     *
     * [하는 일]
     * Redis에 저장된 각 게시글의 좋아요 수를 DB에 반영해.
     * 이렇게 하면 DB에서도 정확한 좋아요 수를 알 수 있어.
     */
    @Scheduled(fixedDelayString = "${community.scheduler.like-sync-delay:300000}", initialDelayString = "${community.scheduler.like-sync-delay:310000}")
    public void syncPostLikeCounts() {
        Map<Long, Integer> likeCounts = redisService.scanAndCollectPostLikeCounts();

        if (likeCounts.isEmpty()) {
            return;
        }

        syncCountsToDb(likeCounts, "post like",
                (postId, count) -> communityPostRepository.setLikeCount(postId, count),
                redisService::deletePostLikeKeys);

        log.info("Post like count sync completed - processed: {}", likeCounts.size());
    }

    /**
     * [댓글 좋아요 이벤트 동기화]
     * 게시글 좋아요와 같은 로직이야. 댓글에 달린 좋아요를 DB에 반영.
     */
    @Scheduled(fixedDelayString = "${community.scheduler.like-sync-delay:300000}")
    public void syncCommentLikeEvents() {
        List<LikeEvent> events = redisService.pollCommentLikeEvents(1000);

        if (events.isEmpty()) {
            return;
        }

        int[] counts = syncLikeEvents(events,
                commentLikeRepository::insertCommentLike,
                commentLikeRepository::deleteByCommentIdAndUserId);

        redisService.trimCommentLikeEvents(events.size());

        log.info("Comment like events synced - total: {}, added: {}, removed: {}",
                events.size(), counts[0], counts[1]);
    }

    /**
     * [댓글 좋아요 개수 동기화]
     * Redis에 저장된 각 댓글의 좋아요 수를 DB에 반영.
     */
    @Scheduled(fixedDelayString = "${community.scheduler.like-sync-delay:300000}", initialDelayString = "${community.scheduler.like-sync-delay:320000}")
    public void syncCommentLikeCounts() {
        Map<Long, Integer> likeCounts = redisService.scanAndCollectCommentLikeCounts();

        if (likeCounts.isEmpty()) {
            return;
        }

        // 트랜잭션 안에서 실행 (하나라도 실패하면 전체 롤백)
        transactionTemplate.executeWithoutResult(status -> {
            likeCounts.forEach((commentId, likeCount) -> {
                try {
                    // 댓글의 좋아요 수 업데이트
                    int affected = commentRepository.setLikeCount(commentId, likeCount);
                    // 업데이트된 row가 없으면 = 댓글이 삭제됨
                    if (affected == 0) {
                        // Redis에서도 해당 키 삭제 (정리)
                        redisService.deleteCommentLikeKeys(commentId);
                    }
                } catch (Exception e) {
                    log.error("Failed to sync comment like count: {}", commentId, e);
                }
            });
        });

        log.info("Comment like count sync completed - processed: {}", likeCounts.size());
    }

    // ============================================================
    // [내부 헬퍼 메서드들 - 중복 코드를 줄이기 위한 공통 로직]
    // ============================================================

    /**
     * [카운터를 DB에 저장하는 공통 메서드]
     *
     * @param counts  저장할 데이터 (ID -> 개수)
     * @param type    로그에 찍을 타입명 (view, comment, like 등)
     * @param updater DB 업데이트 함수
     * @param deleter 성공 후 Redis 키 삭제 함수
     */
    private void syncCountsToDb(Map<Long, Integer> counts, String type,
            CountUpdater updater, KeyDeleter deleter) {
        // 트랜잭션 안에서 실행 (중간에 실패하면 롤백)
        transactionTemplate.executeWithoutResult(status -> {
            counts.forEach((id, count) -> {
                try {
                    // DB에 업데이트하고, 영향받은 row 수 확인
                    int affected = updater.update(id, count);
                    if (affected > 0) {
                        log.debug("{} count synced - id: {}, count: {}", type, id, count);
                    } else {
                        // 업데이트된 row 없음 = 게시글 삭제됨
                        // Redis 키도 삭제하여 정리
                        deleter.delete(id);
                    }
                } catch (Exception e) {
                    // 하나 실패해도 나머지는 계속 진행
                    log.error("Failed to sync {} count for id: {}", type, id, e);
                }
            });
        });
    }

    /**
     * [좋아요 이벤트를 DB에 반영하는 공통 메서드]
     *
     * @param events   좋아요 이벤트 리스트
     * @param inserter 좋아요 추가 함수
     * @param deleter  좋아요 삭제 함수
     * @return [추가된 수, 삭제된 수]
     */
    private int[] syncLikeEvents(List<LikeEvent> events,
            LikeInserter inserter, LikeDeleter deleter) {
        // 중복 제거: 같은 사람이 같은 게시글에 여러 번 좋아요/취소 했으면 마지막 상태만 반영
        // 예: "좋아요 누름 -> 취소 -> 다시 좋아요" 하면 최종 "좋아요" 상태만 처리
        Map<String, LikeEvent> deduplicated = events.stream()
                .collect(Collectors.toMap(
                        e -> e.targetId() + ":" + e.userId(), // 키: "게시글ID:유저ID"
                        e -> e,
                        (old, newer) -> newer)); // 중복이면 최신 이벤트 유지

        int[] counts = { 0, 0 }; // [추가 수, 삭제 수]

        transactionTemplate.executeWithoutResult(status -> {
            deduplicated.values().forEach(event -> {
                try {
                    if (event.isAdd()) {
                        // 좋아요 추가 -> DB에 INSERT
                        inserter.insert(event.targetId(), event.userId());
                        counts[0]++;
                    } else {
                        // 좋아요 취소 -> DB에서 DELETE
                        deleter.delete(event.targetId(), event.userId());
                        counts[1]++;
                    }
                } catch (Exception e) {
                    log.error("Failed to sync like event: {}", event, e);
                }
            });
        });
        return counts;
    }

    // ============================================================
    // [함수형 인터페이스 - 람다식으로 함수를 전달하기 위한 타입 정의]
    // 자바에서 함수를 변수처럼 전달하고 싶을 때 사용해.
    // ============================================================

    @FunctionalInterface
    private interface CountUpdater {
        // ID와 개수를 받아서 DB 업데이트하고, 영향받은 row 수 반환
        int update(Long id, Integer count);
    }

    @FunctionalInterface
    private interface KeyDeleter {
        // ID로 Redis 키 삭제
        void delete(Long id);
    }

    @FunctionalInterface
    private interface LikeInserter {
        // 대상 ID와 유저 ID로 좋아요 추가
        void insert(Long targetId, Long userId);
    }

    @FunctionalInterface
    private interface LikeDeleter {
        // 대상 ID와 유저 ID로 좋아요 삭제
        void delete(Long targetId, Long userId);
    }
}