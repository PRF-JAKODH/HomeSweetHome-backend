package com.homesweet.homesweetback.common.util;

import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentApiClient {

    private final RestTemplate restTemplate;

    /**
     * 외부 API에 POST 요청을 보냅니다. (재시도 로직 적용)
     * * @param url 요청할 URL
     * @param requestEntity 헤더와 바디가 담긴 엔티티
     * @return 응답 Map
     */
    // 👇 [핵심] 실패 시 application.yml의 설정대로 최대 3번까지 알아서 재시도합니다.
    @Retry(name = "toss-payments-retry")
    public Map<String, Object> sendPostRequest(String url, HttpEntity<?> requestEntity) {
        log.debug("API 호출 시도: {}", url); // 재시도할 때마다 로그가 찍힘
        return restTemplate.postForObject(url, requestEntity, Map.class);
    }
}