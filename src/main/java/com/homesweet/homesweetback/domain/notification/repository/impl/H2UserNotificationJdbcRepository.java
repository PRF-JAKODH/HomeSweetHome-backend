package com.homesweet.homesweetback.domain.notification.repository.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homesweet.homesweetback.domain.notification.repository.UserNotificationJdbcRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 테스트 전용 JDBCTemplate Repository
 * 
 * H2에서 JSON을 저장하는 것이 mysql과 달라, 효율을 위해 H2 적용
 */
@Repository
@Profile("test")
public class H2UserNotificationJdbcRepository extends UserNotificationJdbcRepository {

    public H2UserNotificationJdbcRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        super(jdbcTemplate, objectMapper);
    }

    @Override
    protected String getJsonPlaceholder() {
        return "? FORMAT JSON";
    }
}
