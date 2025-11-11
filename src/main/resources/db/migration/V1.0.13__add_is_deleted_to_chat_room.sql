ALTER TABLE chat_room
    ADD COLUMN is_deleted BOOLEAN DEFAULT false NOT NULL;

ALTER TABLE chat_room MODIFY COLUMN thumbnail_url VARCHAR(500);

ALTER TABLE chat_room
    DROP COLUMN last_message_id;

ALTER TABLE chat_room
    ADD COLUMN last_message VARCHAR(1000),
    ADD COLUMN last_message_sent_at TIMESTAMP;
