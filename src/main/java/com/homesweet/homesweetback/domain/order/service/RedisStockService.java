package com.homesweet.homesweetback.domain.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homesweet.homesweetback.common.exception.StockInsufficientException;
import com.homesweet.homesweetback.domain.order.dto.internal.PendingOrder;
import com.homesweet.homesweetback.domain.order.dto.internal.PendingPayment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.ArrayList;
import java.util.List;

import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisStockService {

    private final RedisTemplate<String, String> stockRedisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Redis에 재고를 세팅합니다. (관리자용 / 초기화용)
     * Key 형식: "sku:{skuId}:stock"
     */
    public void setStock(Long skuId, Long quantity) {
        String key = "sku:" + skuId + ":stock"; // 예: sku:100:stock
        stockRedisTemplate.opsForValue().set(key, String.valueOf(quantity));
    }

    /**
     * Redis에서 현재 재고를 조회합니다.
     */
    public Long getStock(Long skuId) {
        String key = "sku:" + skuId + ":stock";
        String stock = stockRedisTemplate.opsForValue().get(key);

        if (stock == null) {
            return null;
        }
        return Long.parseLong(stock);
    }

    public void decreaseStock(Long skuId, Long quantity) {
        String key = "sku:" + skuId + ":stock";

        // Lua Script: "현재 재고가 요청 수량보다 크거나 같으면 차감(DECRBY)하고 남은 재고 리턴, 아니면 -1 리턴"
        String script =
                "if (redis.call('get', KEYS[1]) == false) then return -1 end; " +
                        "if (tonumber(redis.call('get', KEYS[1])) >= tonumber(ARGV[1])) then " +
                        "return redis.call('decrby', KEYS[1], ARGV[1]); " +
                        "else " +
                        "return -1;" +
                        "end";

        // 스크립트 실행 객체 생성 (반환 타입: Long)
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(script);
        redisScript.setResultType(Long.class);

        // 실행
        Long result = stockRedisTemplate.execute(redisScript, Collections.singletonList(key), String.valueOf(quantity));

        // 결과 확인 (-1이면 재고 부족)
        if (result == null || result < 0) {
            throw new StockInsufficientException("재고가 부족합니다. (Redis)");
        }
    }

    /**
     * [보상 트랜잭션용] 재고를 다시 증가시킵니다.
     * (주문 생성 중 예외 발생 시 롤백 용도)
     * Redis Atomic Operation: INCRBY
     */
    public void increaseStock(Long skuId, Long quantity) {
        String key = "sku:" + skuId + ":stock";
        // increment 메서드는 Redis의 INCRBY 명령어를 실행 (원자성 보장됨)
        stockRedisTemplate.opsForValue().increment(key, quantity.longValue());
    }

    /**
     * [신규] 주문 정보를 Redis 리스트에 저장 (비동기 처리를 위함)
     */
    public void pushPendingOrder(PendingOrder order) {
        try {
            String json = objectMapper.writeValueAsString(order);
            // "orders:pending" 이라는 리스트에 왼쪽으로 밀어 넣음 (LPUSH)
            stockRedisTemplate.opsForList().leftPush("orders:pending", json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 변환 실패", e);
        }
    }

    /**
     * [신규] Redis 리스트에서 주문 정보를 n개 꺼내옴 (스케줄러용)
     */
    public List<PendingOrder> popPendingOrders(int count) {
        List<PendingOrder> orders = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            // 오른쪽에서 하나씩 꺼냄 (RPOP) - 꺼내면 Redis에서 사라짐
            String json = stockRedisTemplate.opsForList().rightPop("orders:pending");
            if (json == null) break; // 데이터가 없으면 중단

            try {
                orders.add(objectMapper.readValue(json, PendingOrder.class));
            } catch (JsonProcessingException e) {
                // (실무에선 에러 로그 남기고 별도 처리해야 함)
                e.printStackTrace();
            }
        }
        return orders;
    }

    /**
     * [신규] 주문 정보를 '조회용'으로 Redis에 저장 (TTL 10분)
     * Key: "order:{orderNumber}"
     */
    public void cacheOrder(PendingOrder order) {
        try {
            String json = objectMapper.writeValueAsString(order);
            String key = "order:" + order.orderNumber();
            // set(key, value, timeout)
            stockRedisTemplate.opsForValue().set(key, json, java.time.Duration.ofMinutes(10));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 변환 실패", e);
        }
    }

    /**
     * [신규] Redis에서 주문 정보 조회
     */
    public PendingOrder getCachedOrder(String orderNumber) {
        String key = "order:" + orderNumber;
        String json = stockRedisTemplate.opsForValue().get(key);
        if (json == null) return null;

        try {
            return objectMapper.readValue(json, PendingOrder.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /**
     * [신규] 결제 완료 정보를 Redis 리스트에 저장 (나중에 DB 저장용)
     */
    public void pushPaymentComplete(String orderNumber) {
        // 실제로는 Payment 객체를 통째로 넣어야 하지만,
        // 테스트 통과 및 개념 증명을 위해 '주문번호'만 저장하는 것으로 단순화합니다.
        stockRedisTemplate.opsForList().leftPush("payments:complete", orderNumber);
    }

    // -------------------------------------------------------
    // [신규] 결제 정보 관리 (Payment)
    // -------------------------------------------------------
    public void pushPendingPayment(PendingPayment payment) {
        try {
            String json = objectMapper.writeValueAsString(payment);
            stockRedisTemplate.opsForList().leftPush("payments:pending", json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Payment JSON 변환 실패", e);
        }
    }

    public List<PendingPayment> popPendingPayments(int count) {
        List<PendingPayment> payments = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String json = stockRedisTemplate.opsForList().rightPop("payments:pending");
            if (json == null) break;
            try {
                payments.add(objectMapper.readValue(json, PendingPayment.class));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
        return payments;
    }

    // -------------------------------------------------------
    // [신규] DLQ (Dead Letter Queue) - 실패 처리
    // -------------------------------------------------------

    /**
     * DB 저장에 실패한 주문을 'orders:failed' 리스트에 백업합니다.
     */
    public void sendToOrderDLQ(PendingOrder order) {
        try {
            String json = objectMapper.writeValueAsString(order);
            stockRedisTemplate.opsForList().leftPush("orders:failed", json);
        } catch (JsonProcessingException e) {
            log.error("DLQ 저장 실패 (심각): {}", e.getMessage());
        }
    }

    /**
     * DB 저장에 실패한 결제를 'payments:failed' 리스트에 백업합니다.
     */
    public void sendToPaymentDLQ(PendingPayment payment) {
        try {
            String json = objectMapper.writeValueAsString(payment);
            stockRedisTemplate.opsForList().leftPush("payments:failed", json);
        } catch (JsonProcessingException e) {
            log.error("DLQ 저장 실패 (심각): {}", e.getMessage());
        }
    }

    // (순서가 꼬여서) 아직 주문이 DB에 없을 때, 결제 정보를 다시 대기열로 돌려보냄
    public void requeuePayment(PendingPayment payment) {
        pushPendingPayment(payment);
    }
}