
DELETE FROM notification_template 
WHERE template_type IN ('NEW_FOLLOW', 'SETTLEMENT_FAILED', 'CHAT_ROOM_INVITE');

-- NEW_COMMENT_LIKE (COMMUNITY)
INSERT INTO notification_template (notification_category_id, template_type, title, content, redirect_url)
SELECT c.notification_category_id, 'NEW_COMMENT_LIKE', '새 댓글 좋아요',
       '{userName}님이 댓글에 좋아요를 눌렀습니다.',
       '/community/posts/{postId}'
FROM notification_category c
WHERE c.category_name = 'COMMUNITY'
  AND NOT EXISTS (
    SELECT 1 FROM notification_template t WHERE t.template_type = 'NEW_COMMENT_LIKE'
  );

-- SETTLEMENT_FAILED (SETTLEMENT)
INSERT INTO notification_template (notification_category_id, template_type, title, content, redirect_url)
SELECT c.notification_category_id, 'SETTLEMENT_FAILED', '정산 실패',
       '{userName}님의 {settlementName} 정산이 실패했습니다. (정산 ID: {settlementId})',
       '/settlements/{settlementId}'
FROM notification_category c
WHERE c.category_name = 'SETTLEMENT'
  AND NOT EXISTS (
    SELECT 1 FROM notification_template t WHERE t.template_type = 'SETTLEMENT_FAILED'
  );

-- NEW_REVIEW (PRODUCT)
INSERT INTO notification_template (notification_category_id, template_type, title, content, redirect_url)
SELECT c.notification_category_id, 'NEW_REVIEW', '새 리뷰 등록',
       '{userName}님이 {productName} 상품에 리뷰를 등록했습니다.',
       '/products/{productId}'
FROM notification_category c
WHERE c.category_name = 'PRODUCT'
  AND NOT EXISTS (
    SELECT 1 FROM notification_template t WHERE t.template_type = 'NEW_REVIEW'
  );

-- CUSTOM (판매자 등록 완료)
INSERT INTO notification_template (notification_category_id, template_type, title, content, redirect_url)
SELECT c.notification_category_id, 'SELLER_REGISTRATION_COMPLETE', '판매자 등록 완료',
       '판매자 등록이 완료되었습니다.',
       '/seller'
FROM notification_category c
WHERE c.category_name = 'SYSTEM'
  AND NOT EXISTS (
    SELECT 1 FROM notification_template t WHERE t.template_type = 'SELLER_REGISTRATION_COMPLETE' AND t.title = '판매자 등록 완료'
  );

