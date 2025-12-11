package com.homesweet.homesweetback.domain.settlement.batch.step.create;

import com.homesweet.homesweetback.common.exception.BusinessException;
import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.order.entity.Order;
import com.homesweet.homesweetback.domain.order.repository.OrderRepository;
import com.homesweet.homesweetback.domain.settlement.dto.response.SettlementCreateDto;
import com.homesweet.homesweetback.domain.settlement.entity.Settlement;
import com.homesweet.homesweetback.domain.settlement.repository.SettlementRepository;
import com.homesweet.homesweetback.domain.settlement.util.ExtractedSeller;
import com.homesweet.homesweetback.domain.settlement.util.calculator.SettlementCalculator;
import com.homesweet.homesweetback.domain.settlement.validation.SettlementValidator;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * SettlementCreateProcessor
 * - Reader가 전달한 Order를 Settlement 엔티티로 변환하는 Processor
 * - 수수료 / VAT / 환불 / 최종 정산금액 계산을 수행
 */
@Slf4j
//@Component
@StepScope
@RequiredArgsConstructor
public class SettlementCreateProcessor implements ItemProcessor<SettlementCreateDto, Settlement> {
    private final SettlementCalculator settlementCalculator;
    private final SettlementValidator settlementValidator;
    private final SettlementRepository settlementRepository;
    private Map<Long, User> sellerCache;
    private final MeterRegistry meterRegistry;
    private boolean cacheLoaded = false;

    @PostConstruct
    public void init() {
        long start = System.currentTimeMillis();
        sellerCache = new HashMap<>();
        try {
            settlementRepository.findAllBySellerRole().forEach(seller ->
                    sellerCache.put(seller.getId(), seller)
            );
        log.info("SellerCache 초기화 완료 size: {}", sellerCache.size());
        } catch (Exception e) {
            log.error("SellerCache 로딩 실패: {}", e.getMessage());
            throw e;
        }finally {
            long duration = System.currentTimeMillis() - start;

            meterRegistry.timer("batch_processor_seller_cache_load_duration")
                    .record(duration, java.util.concurrent.TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public Settlement process(SettlementCreateDto dto) {
//        init();
        long start = System.currentTimeMillis();
        try {

        User seller = sellerCache.get(dto.sellerId());
        log.info("Processor called for order = {}", dto.orderId());
        if (seller == null) {
            meterRegistry.counter("batch_processor_missing_seller_count").increment();
            log.error("[Processor] Seller not found for ID={}", dto.sellerId());
            throw new BusinessException(ErrorCode.SELLER_NOT_FOUND);
        }
        // 1. 판매자 추출
//        User seller = extractedSeller.extractSeller(order);
        // 100만번 부르는 중-> map 처리
//        User seller = settlementRepository.findBySellerId(sellerId).orElseThrow(() -> new BusinessException(ErrorCode.SELLER_NOT_FOUND));


//        settlementValidator.validateSeller(dto.sellerId());
        // Order은 영속성 객체로 가져오기
//        Order order = orderRepository.getReferenceById(dto.orderId());

        // 2. 정산 금액 계산
        SettlementCalculator.Result result = settlementCalculator.getResult(dto, seller);
        settlementValidator.validateResultNotNull(result);

        meterRegistry.counter("batch_processor_success_count").increment();

        // 3. Settlement 엔티티 생성
        return Settlement.builder()
                .settlementId(UUID.randomUUID())
                .orderId(dto.orderId())
                .userId(dto.sellerId())
                .salesAmount(result.totalAmount())
                .fee(result.fee())
                .vat(result.vat())
                .refundAmount(result.refundAmount())
                .settlementAmount(result.settlementAmount())
                .settlementStatus("PENDING")
                .settlementDate(LocalDateTime.now())
                .build();
        } catch (Exception e) {
            meterRegistry.counter("batch_processor_error_count").increment();
            throw e;

        } finally {

            long duration = System.currentTimeMillis() - start;

            meterRegistry.timer("batch_processor_process_duration")
                    .record(duration, java.util.concurrent.TimeUnit.MILLISECONDS);
        }
    }
}
