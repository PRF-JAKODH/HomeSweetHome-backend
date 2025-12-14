package com.homesweet.homesweetback.domain.order.service;

import com.homesweet.homesweetback.common.config.TossPaymentsConfig;
import com.homesweet.homesweetback.common.exception.TossApiFailedException;
import com.homesweet.homesweetback.common.util.PaymentApiClient;
import com.homesweet.homesweetback.domain.order.dto.TossPaymentCancelRequest;
import com.homesweet.homesweetback.domain.order.dto.TossPaymentConfirmRequest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 토스페이먼츠 API 연동 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TossPaymentsService {

    private final TossPaymentsConfig tossPaymentsConfig;
    private final PaymentApiClient paymentApiClient;
    private final RestTemplate restTemplate;

    /**
     * 결제 승인 요청
     * 프론트엔드에서 결제창 완료 후 받은 paymentKey, orderId, amount로 최종 승인
     */
    @CircuitBreaker(name = "toss-payments", fallbackMethod = "confirmFallback")
    public Map<String, Object> confirmPayment(TossPaymentConfirmRequest request) {
        log.info("결제 승인 요청: orderId={}, amount={}", request.getOrderId(), request.getAmount());

        Map<String, Object> body = new HashMap<>();
        body.put("paymentKey", request.getPaymentKey());
        body.put("orderId", request.getOrderId());
        body.put("amount", request.getAmount());

        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(body, createHeaders());

        try {
            Map<String, Object> response = paymentApiClient.sendPostRequest(
                    tossPaymentsConfig.getConfirmUrl(),
                    httpEntity);
            log.info("결제 승인 성공: paymentKey={}", request.getPaymentKey());
            return response;
        } catch (HttpClientErrorException e) {
            log.error("결제 승인 실패: {}", e.getResponseBodyAsString());
            throw new TossApiFailedException("결제 승인 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 결제 취소 요청
     */
    @CircuitBreaker(name = "toss-payments", fallbackMethod = "cancelFallback")
    public Map<String, Object> cancelPayment(String paymentKey, TossPaymentCancelRequest request) {
        log.info("결제 취소 요청: paymentKey={}, reason={}", paymentKey, request.getCancelReason());

        Map<String, Object> body = new HashMap<>();
        body.put("cancelReason", request.getCancelReason());
        if (request.getCancelAmount() != null) {
            body.put("cancelAmount", request.getCancelAmount());
        }

        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(body, createHeaders());

        try {
            Map<String, Object> response = paymentApiClient.sendPostRequest(
                    tossPaymentsConfig.getCancelUrl(paymentKey),
                    httpEntity);
            log.info("결제 취소 성공: paymentKey={}", paymentKey);
            return response;
        } catch (HttpClientErrorException e) {
            log.error("결제 취소 실패: {}", e.getResponseBodyAsString());
            throw new TossApiFailedException("결제 취소 실패: " + e.getMessage(), e);
        }
    }

    /**
     * paymentKey로 결제 조회
     */
    @CircuitBreaker(name = "toss-payments", fallbackMethod = "queryFallback")
    public Map<String, Object> getPaymentByPaymentKey(String paymentKey) {
        log.info("결제 조회 요청: paymentKey={}", paymentKey);

        HttpEntity<Void> httpEntity = new HttpEntity<>(createHeaders());

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    tossPaymentsConfig.getPaymentUrl(paymentKey),
                    HttpMethod.GET,
                    httpEntity,
                    Map.class);
            log.info("결제 조회 성공: paymentKey={}", paymentKey);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("결제 조회 실패: {}", e.getResponseBodyAsString());
            throw new TossApiFailedException("결제 조회 실패: " + e.getMessage(), e);
        }
    }

    /**
     * orderId로 결제 조회
     */
    @CircuitBreaker(name = "toss-payments", fallbackMethod = "queryFallback")
    public Map<String, Object> getPaymentByOrderId(String orderId) {
        log.info("주문ID로 결제 조회 요청: orderId={}", orderId);

        HttpEntity<Void> httpEntity = new HttpEntity<>(createHeaders());

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    tossPaymentsConfig.getOrderIdUrl(orderId),
                    HttpMethod.GET,
                    httpEntity,
                    Map.class);
            log.info("주문ID로 결제 조회 성공: orderId={}", orderId);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("주문ID로 결제 조회 실패: {}", e.getResponseBodyAsString());
            throw new TossApiFailedException("결제 조회 실패: " + e.getMessage(), e);
        }
    }

    /**
     * Basic 인증 헤더 생성
     * 토스페이먼츠는 시크릿키 + ":" 를 Base64 인코딩해서 Authorization 헤더에 전달
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String credentials = tossPaymentsConfig.getSecretKey() + ":";
        String encodedCredentials = Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        headers.set("Authorization", "Basic " + encodedCredentials);

        return headers;
    }

    // ===== Fallback 메서드 =====

    private Map<String, Object> confirmFallback(TossPaymentConfirmRequest request, Throwable t) {
        log.error("결제 승인 서킷브레이커 작동: orderId={}, error={}", request.getOrderId(), t.getMessage());
        throw new TossApiFailedException("결제 서비스가 일시적으로 불안정합니다. 잠시 후 다시 시도해주세요.", t);
    }

    private Map<String, Object> cancelFallback(String paymentKey, TossPaymentCancelRequest request, Throwable t) {
        log.error("결제 취소 서킷브레이커 작동: paymentKey={}, error={}", paymentKey, t.getMessage());
        throw new TossApiFailedException("결제 취소 서비스가 일시적으로 불안정합니다. 잠시 후 다시 시도해주세요.", t);
    }

    private Map<String, Object> queryFallback(String key, Throwable t) {
        log.error("결제 조회 서킷브레이커 작동: key={}, error={}", key, t.getMessage());
        throw new TossApiFailedException("결제 조회 서비스가 일시적으로 불안정합니다. 잠시 후 다시 시도해주세요.", t);
    }
}
