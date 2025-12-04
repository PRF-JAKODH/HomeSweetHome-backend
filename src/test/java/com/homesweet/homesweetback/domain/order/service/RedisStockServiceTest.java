//package com.homesweet.homesweetback.domain.order.service;
//
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.ActiveProfiles;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//@SpringBootTest
//@ActiveProfiles("test") // (로컬 Redis 연결 설정을 위해)
//class RedisStockServiceTest {
//
//    @Autowired
//    private RedisStockService redisStockService;
//
//    @Test
//    @DisplayName("Redis에 재고를 저장하고 조회할 수 있다.")
//    void setAndGetStock() {
//        // --- GIVEN ---
//        Long skuId = 100L;
//        Long quantity = 5000L;
//
//        // --- WHEN ---
//        // Redis에 재고 5000개 세팅!
//        redisStockService.setStock(skuId, quantity);
//
//        // --- THEN ---
//        // 다시 조회해서 5000개가 맞는지 확인
//        Long savedStock = redisStockService.getStock(skuId);
//
//        System.out.println("Redis 저장된 재고: " + savedStock);
//        assertThat(savedStock).isEqualTo(quantity);
//    }
//
//    @Test
//    @DisplayName("Lua Script를 사용하여 재고 차감에 성공한다.")
//    void decreaseStock_Success() {
//        // GIVEN
//        Long skuId = 200L;
//        Long initialStock = 100L;
//        Long decreaseAmount = 5L;
//
//        // 초기 재고 세팅
//        redisStockService.setStock(skuId, initialStock);
//
//        // WHEN
//        redisStockService.decreaseStock(skuId, decreaseAmount);
//
//        // THEN
//        Long currentStock = redisStockService.getStock(skuId);
//        assertThat(currentStock).isEqualTo(95L); // 100 - 5 = 95
//    }
//
//    @Test
//    @DisplayName("재고보다 많은 수량을 차감하면 예외가 발생한다.")
//    void decreaseStock_Fail_NotEnough() {
//        // GIVEN
//        Long skuId = 300L;
//        Long initialStock = 10L;
//        Long decreaseAmount = 11L; // 재고보다 1개 많음
//
//        redisStockService.setStock(skuId, initialStock);
//
//        // WHEN & THEN
//        // 예외가 발생해야 함 (재고는 그대로 10이어야 함)
//        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
//                redisStockService.decreaseStock(skuId, decreaseAmount)
//        ).isInstanceOf(com.homesweet.homesweetback.common.exception.StockInsufficientException.class);
//
//        // 재고가 차감되지 않았는지 확인
//        Long currentStock = redisStockService.getStock(skuId);
//        assertThat(currentStock).isEqualTo(10L);
//    }
//}
