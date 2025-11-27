package com.homesweet.homesweetback.domain.community.event;

import com.homesweet.homesweetback.domain.search.community.sync.service.CommunitySyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 게시글 이벤트 Consumer
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 27.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommunityEventConsumer {

    private final CommunitySyncService communitySyncService;

    @Async("communityEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void consume(CommunityEvent event) {
        log.info("[게시글 이벤트] 실행: type={}, communityId={}", event.getEventType(), event.getCommunityId());

        try {
            switch (event.getCommunityEventType()) {
                case CREATED, UPDATED -> communitySyncService.syncToElasticsearch(event.getCommunityId());
                case DELETED -> communitySyncService.deleteFromElasticsearch(event.getCommunityId());
            }
        } catch (Exception e) {
            log.error("Failed to handle community event: {}", event, e);
        }
    }
}
