package com.homesweet.homesweetback.domain.settlement.repository.querydsl;

import com.homesweet.homesweetback.domain.settlement.entity.QSettlement;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Repository
@RequiredArgsConstructor
public class CustomSettlementRepositoryImpl implements CustomSettlementRepository {
    private final JPAQueryFactory jpaQueryFactory;
    private final QSettlement qSettlement = QSettlement.settlement;

    // 환불 금액 계산
    @Override
    @Transactional
    public int applyRefundAmount(Long orderId, BigDecimal refundAmount) {
        return (int) jpaQueryFactory
                .update(qSettlement)
                .set(qSettlement.refundAmount, qSettlement.refundAmount.add(refundAmount))
                .set(qSettlement.settlementAmount, qSettlement.salesAmount
                        .add(qSettlement.vat)
                        .subtract(qSettlement.fee)
                        .subtract(qSettlement.refundAmount.add(refundAmount)))
                .set(qSettlement.settlementStatus, "CANCELED")
                .where(qSettlement.order.id.eq(orderId))
                .execute();
    }
}
