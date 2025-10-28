package com.homesweet.homesweetback.domain.notification.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.PathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.ActiveProfiles;

import com.homesweet.homesweetback.domain.notification.entity.NotificationTemplate;

import java.sql.Connection;
import java.util.List;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class NotificationTemplateRepositoryTest {
    
    @Autowired
    private NotificationTemplateRepository notificationTemplateRepository;

    @Autowired
    private DataSource dataSource;

    @BeforeEach
    public void dbSetup() throws Exception {          
        try (Connection conn = dataSource.getConnection()) {             
            ScriptUtils.executeSqlScript(conn, new PathResource("src/main/resources/db/data.sql"));          
        }    
    }

    @Test
    @DisplayName("알림 템플릿 타입 조회")
    public void get_all_notification_templates() {
        List<NotificationTemplate> notificationTemplates = notificationTemplateRepository.findAll();
        notificationTemplates.stream().map(NotificationTemplate::getTemplateType).forEach(System.out::println);
        assertThat(notificationTemplates).isNotEmpty();
    }

}
