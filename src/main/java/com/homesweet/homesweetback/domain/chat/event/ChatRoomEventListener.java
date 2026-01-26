package com.homesweet.homesweetback.domain.chat.event;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.format.DateTimeFormatter;


@Component
@RequiredArgsConstructor
@Slf4j
public class ChatRoomEventListener {

    private final SimpMessagingTemplate messagingTemplate;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * 멤버 변경 이벤트 처리
     * - 트랜잭션 커밋 후 실행
     * - 채팅방의 모든 구독자에게 브로드캐스트
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMemberUpdate(ChatRoomDataUpdateEvent event) {

        // WebSocket 목적지 (채팅방별 구독 경로)
        String destination = "/sub/chat/rooms/" + event.roomId();

        // ChatRoomUpdateData로 래핑
        ChatRoomUpdateData updateData = new ChatRoomUpdateData(
                event.roomId(),
                event.updateType().name(),  // "MEMBER_JOINED" or "MEMBER_LEFT"
                event.data(),
                event.occurredAt().format(FORMATTER)
        );

        // WebSocketMessage로 한 번 더 래핑
        WebSocketMessage message = new WebSocketMessage(
                "CHAT_ROOM_UPDATE",  // type
                updateData // data
        );

        // 모든 구독자에게 전송
        messagingTemplate.convertAndSend(destination, message);

        log.info("멤버 변경 브로드캐스트. destination={}, type={}",
                destination, event.updateType().name());
    }
}