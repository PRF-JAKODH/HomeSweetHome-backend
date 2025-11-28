package com.homesweet.homesweetback.domain.search.chat.event;

import com.homesweet.homesweetback.domain.search.chat.sync.service.ChatroomSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 채팅방 검색 관련 이벤트 리스너
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 27.
 */
@Profile("!test")
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "search.elasticsearch.enabled", havingValue = "true")
public class ChatroomSearchEventConsumer {

    private final ChatroomSyncService chatroomSyncService;

    @Async("chatroomEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void consume(ChatroomEvent event) {
        log.info("[채팅방 이벤트] 실행: type={}, community={}", event.getEventType(), event.getChatroomId());

        try {
            switch (event.getChatroomEventType()) {
                case CREATED, UPDATED -> chatroomSyncService.syncToElasticsearch(event.getChatroomId());
                case DELETED -> chatroomSyncService.deleteFromElasticsearch(event.getChatroomId());
            }
        } catch (Exception e) {
            log.error("Failed to handle community event: {}", event, e);
        }
    }
}
