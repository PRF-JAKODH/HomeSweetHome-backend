package com.homesweet.homesweetback.domain.notification.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homesweet.homesweetback.domain.notification.entity.UserNotification;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public abstract class UserNotificationJdbcRepository {

    protected final JdbcTemplate jdbcTemplate;
    protected final ObjectMapper objectMapper;

    private static final String BULK_INSERT_SQL = "INSERT INTO user_notification (user_id, notification_template_id, context_data, is_read, is_deleted, created_at) VALUES ";

    protected abstract String getJsonPlaceholder();

    @Transactional
    public void saveAll(List<UserNotification> notifications) {
        if (notifications.isEmpty()) {
            return;
        }

        int batchSize = 1000; // 쿼리 길이 제한을 고려하여 1000개씩 끊어서 처리
        for (int i = 0; i < notifications.size(); i += batchSize) {
            List<UserNotification> batchList = notifications.subList(i, Math.min(i + batchSize, notifications.size()));
            bulkInsertBatch(batchList);
        }
    }

    private void bulkInsertBatch(List<UserNotification> notifications) {
        StringBuilder sql = new StringBuilder(BULK_INSERT_SQL);

        List<Object> params = new ArrayList<>();

        for (int i = 0; i < notifications.size(); i++) {
            UserNotification n = notifications.get(i);
            sql.append(String.format("(?, ?, %s, ?, ?, ?)", getJsonPlaceholder()));
            if (i < notifications.size() - 1) {
                sql.append(", ");
            }

            params.add(n.getUser().getId());
            params.add(n.getTemplate().getId());
            try {
                // JPA처럼 Map을 JSON String으로 변환하여 전달
                params.add(objectMapper.writeValueAsString(n.getContextData()));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize contextData", e);
            }
            params.add(n.getIsRead());
            params.add(n.getIsDeleted());
            params.add(LocalDateTime.now()); // CreatedAt 설정
        }

        // 1. Bulk Insert 실행
        jdbcTemplate.update(sql.toString(), params.toArray());

        // 2. 첫 번째 ID 조회
        Long lastInsertId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

        // 3. ID 할당
        if (lastInsertId != null) {
            long startId = lastInsertId;
            for (int i = 0; i < notifications.size(); i++) {
                notifications.get(i).setId(startId + i);
            }
        }
    }
}
