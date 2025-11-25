package com.homesweet.homesweetback.domain.settlement.batch.step.create;

import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.grade.service.GradeService;
import com.homesweet.homesweetback.domain.order.entity.Order;
import com.homesweet.homesweetback.domain.settlement.entity.Settlement;
import com.homesweet.homesweetback.domain.settlement.util.ExtractedSeller;
import com.homesweet.homesweetback.domain.settlement.util.calculator.SettlementCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

/**
 * SettlementCreateProcessor
 * - Reader가 전달한 Order를 Settlement 엔티티로 변환하는 Processor
 * - 수수료 / VAT / 환불 / 최종 정산금액 계산을 수행
 */
@Component
@StepScope
@RequiredArgsConstructor
public class SettlementCreateProcessor implements ItemProcessor<Order, Settlement> {
    private final ExtractedSeller extractedSeller;
    private final SettlementCalculator settlementCalculator;

    @Override
    public Settlement process(Order order) {
        // 1. 판매자 추출
        User seller = extractedSeller.extractSeller(order);
        // 2. 정산 금액 계산
        SettlementCalculator.Result result = settlementCalculator.getResult(order, seller);
        // 3. Settlement 엔티티 생성
        return Settlement.builder()
                .order(order)
                .userId(seller.getId())
                .salesAmount(result.totalAmount())
                .fee(result.fee())
                .vat(result.vat())
                .refundAmount(result.refundAmount())
                .settlementAmount(result.settlementAmount())
                .settlementStatus("PENDING")
                .settlementDate(LocalDateTime.now())
                .build();
    }
}
