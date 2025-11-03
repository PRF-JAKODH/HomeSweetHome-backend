-- ====================================
-- 누락된 알림 템플릿 추가
-- ====================================
-- NotificationEventType.java에 정의되어 있으나 DB에 없는 템플릿들을 추가합니다.

-- 알림 템플릿 데이터 삽입 (누락된 항목)
INSERT INTO notification_template (notification_category_id, template_type, title, content, redirect_url) VALUES 
    -- 커뮤니티 관련 (누락)
    (3, 'NEW_FOLLOW', '새 팔로우', '{userName}님이 당신을 팔로우했습니다.', '/users/{followerId}'),
    
    -- 정산 관련 (누락)
    (4, 'SETTLEMENT_FAILED', '정산 실패', '{userName}님의 {settlementName} 정산이 실패했습니다. (정산 ID: {settlementId})', '/settlements/{settlementId}'),
    
    -- 채팅 관련 (누락)
    (6, 'CHAT_ROOM_INVITE', '채팅방 초대', '{userName}님이 {roomName} 채팅방에 초대했습니다.', '/chat/{roomId}')
    
ON DUPLICATE KEY UPDATE 
    title = VALUES(title), 
    content = VALUES(content), 
    redirect_url = VALUES(redirect_url);
