-- ===========================================================
--  ChatRoom / ChatImage / ChatMessage 테이블 구조 수정
--  Author  : junwoo.kim
--  Created : 2025-10-27
-- ===========================================================


-- -----------------------------------------------------------
-- (1) chat_room 테이블 수정
-- -----------------------------------------------------------

-- (1-1) 기존 CHECK 제약 제거 (존재 시)
ALTER TABLE chat_room DROP CHECK chk_chat_room_type;

-- (1-2) 컬럼명 변경: room_type → type
ALTER TABLE chat_room
    CHANGE COLUMN room_type type VARCHAR(100) NOT NULL;

-- (1-3) CHECK 제약 재등록
ALTER TABLE chat_room
    ADD CONSTRAINT chk_chat_room_type
        CHECK (type IN ('INDIVIDUAL', 'GROUP'));

-- (1-4) pair_key 컬럼 추가 (1:1 방 중복 방지용; 그룹은 NULL 허용)
-- ※ IF NOT EXISTS 제거 (MySQL 8.0.29 미만 호환성)
ALTER TABLE chat_room
    ADD COLUMN pair_key VARCHAR(100) NULL
        AFTER name;

-- (1-5) thumbnail_url 길이 변경: 255 → 100
ALTER TABLE chat_room
    MODIFY COLUMN thumbnail_url VARCHAR(100) NULL;

-- (1-6) 더 이상 쓰지 않는 updated_at 컬럼 제거 (존재 시)
ALTER TABLE chat_room
    DROP COLUMN updated_at;

-- (1-7) (type, pair_key) 유니크 제약 재등록
-- ※ 이미 존재 시 수동 삭제 필요
ALTER TABLE chat_room
    ADD CONSTRAINT uk_chat_room_type_pair UNIQUE (type, pair_key);



-- -----------------------------------------------------------
-- (2) chat_image 테이블 수정
-- -----------------------------------------------------------

-- (2-1) file_name 컬럼 추가
ALTER TABLE chat_image
    ADD COLUMN file_name VARCHAR(100) NULL
        AFTER file_type;

-- (2-2) NULL 값 보정 (기존 데이터 안전 처리)
UPDATE chat_image
SET file_name = COALESCE(file_name, 'unknown')
WHERE file_name IS NULL;

-- (2-3) file_name NOT NULL로 변경
ALTER TABLE chat_image
    MODIFY COLUMN file_name VARCHAR(100) NOT NULL;



-- -----------------------------------------------------------
-- (3) chat_message 테이블 수정
-- -----------------------------------------------------------

-- (3-1) message_type 길이 확장 (VARCHAR 100)
ALTER TABLE chat_message
    MODIFY COLUMN message_type VARCHAR(100) NOT NULL;

ALTER TABLE chat_message
    MODIFY COLUMN message_content TEXT NOT NULL;