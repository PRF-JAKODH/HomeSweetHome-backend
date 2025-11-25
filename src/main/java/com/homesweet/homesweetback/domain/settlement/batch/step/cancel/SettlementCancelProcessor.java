package com.homesweet.homesweetback.domain.settlement.batch.step.cancel;

import com.homesweet.homesweetback.common.exception.BusinessException;
import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.order.entity.Order;
import com.homesweet.homesweetback.domain.settlement.entity.Settlement;
import com.homesweet.homesweetback.domain.settlement.repository.SettlementRepository;
import com.homesweet.homesweetback.domain.settlement.util.calculator.SettlementCalculator;
import com.homesweet.homesweetback.domain.settlement.validation.SettlementValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SettlementCancelProcessor implements ItemProcessor<Order, Settlement> {
    private final SettlementRepository settlementRepository;
    private final SettlementValidator settlementValidator;
    private final SettlementCalculator settlementCalculator;

    @Override
    public Settlement process(Order order) {
        // 1. settlement 조회
        Settlement settlement = settlementRepository.findByOrderId(order.getId()).orElseThrow(() -> new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND));
        // 2. 취소 검증
        settlementValidator.validateCanceled(settlement, order);
        // 3. 환불 금액 계산
        settlement.setRefundAmount(settlementCalculator.refundResult(settlement));
        // 4. 정산 금액 재계산
        settlement.setSettlementAmount(settlement.getSalesAmount().subtract(settlement.getFee()).subtract(
                settlement.getSettlementAmount()).subtract(settlement.getVat()));
        // 5. 정산 상태 변경
        settlement.setSettlementStatus("CANCELED");

        return settlement;
    }
}
