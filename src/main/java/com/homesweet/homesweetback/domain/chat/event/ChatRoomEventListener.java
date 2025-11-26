package com.homesweet.homesweetback.domain.chat.event;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;


@Component
@RequiredArgsConstructor
@Slf4j
public class ChatRoomEventListener {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 멤버 변경 이벤트 처리
     * - 트랜잭션 커밋 후 실행
     * - 채팅방의 모든 구독자에게 브로드캐스트
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMemberUpdate(ChatRoomDataUpdateEvent event) {

        // WebSocket 목적지 (채팅방별 구독 경로)
        String destination = "/sub/chat/rooms/" + event.roomId() + "/members";

        // 브로드캐스트 메시지 생성
        WebSocketMessage message = new WebSocketMessage(
                event.updateType().name(),  // "MEMBER_JOINED" or "MEMBER_LEFT"
                event.data(),
                event.occurredAt()
        );

        // 모든 구독자에게 전송
        messagingTemplate.convertAndSend(destination, message);

        log.info("멤버 변경 브로드캐스트. destination={}, type={}",
                destination, event.updateType().name());
    }
}