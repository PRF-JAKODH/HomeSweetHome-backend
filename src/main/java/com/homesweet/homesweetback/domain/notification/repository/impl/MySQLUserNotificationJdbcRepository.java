package com.homesweet.homesweetback.domain.notification.repository.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homesweet.homesweetback.domain.notification.repository.UserNotificationJdbcRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!test")
public class MySQLUserNotificationJdbcRepository extends UserNotificationJdbcRepository {

    public MySQLUserNotificationJdbcRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        super(jdbcTemplate, objectMapper);
    }

    @Override
    protected String getJsonPlaceholder() {
        return "?";
    }
}
