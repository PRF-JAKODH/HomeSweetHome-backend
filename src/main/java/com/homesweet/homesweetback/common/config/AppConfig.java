package com.homesweet.homesweetback.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration // 이 클래스가 Spring 설정 파일임을 알림
public class AppConfig {

    @Bean // 이 메서드가 반환하는 객체를 Spring Bean으로 등록
    public RestTemplate restTemplate() {
        return new RestTemplate(); // RestTemplate 객체를 생성하여 반환
    }

//    @Bean // ObjectMapper도 Bean으로 등록 (Service에서 필요함)
//    public ObjectMapper objectMapper() {
//        return new ObjectMapper();
//    }
}