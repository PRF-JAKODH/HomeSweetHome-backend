package com.homesweet.homesweetback.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * 사용자 로그 프로듀서 서비스
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 10.
 */
@Service
@RequiredArgsConstructor
public class UserLogProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private static final String TOPIC = "user-access-log";

    public void sendLog(String message) {
        kafkaTemplate.send(TOPIC, message);
    }
}
