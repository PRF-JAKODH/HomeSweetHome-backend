package com.homesweet.homesweetback.domain.settlement.repository.querydsl.impl;

import com.homesweet.homesweetback.domain.settlement.entity.QMonthlySettlement;
import com.homesweet.homesweetback.domain.settlement.entity.QWeeklySettlement;
import com.homesweet.homesweetback.domain.settlement.entity.QYearlySettlement;
import com.homesweet.homesweetback.domain.settlement.entity.YearlySettlement;
import com.homesweet.homesweetback.domain.settlement.repository.YearlySettlementRepository;
import com.homesweet.homesweetback.domain.settlement.repository.querydsl.CustomYearlySettlementRepository;
import com.homesweet.homesweetback.domain.settlement.util.vo.SettlementTotals;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Repository
@RequiredArgsConstructor
public class YearlySettlementRepositoryImpl implements CustomYearlySettlementRepository {
    private final JPAQueryFactory jpaQueryFactory;
    private final QMonthlySettlement m = QMonthlySettlement.monthlySettlement;
    private final QYearlySettlement y = QYearlySettlement.yearlySettlement;
    private final EntityManager em;

    @Transactional
    public int upsertYearly(Long userId, Short year, SettlementTotals totals) {
        Long count = jpaQueryFactory
                .select(m.count())
                .from(m)
                .where(
                        m.userId.eq(userId),
                        m.year.eq(year)
                )
                .fetchOne();

        if (count == null || count == 0) {
            return 0; // monthly 데이터가 없으면 INSERT도 하지 않음
        }

        // 1) monthly SUM 조회
        Tuple sums = jpaQueryFactory
                .select(
                        m.totalSales.sum().coalesce(BigDecimal.ZERO),
                        m.totalFee.sum().coalesce(BigDecimal.ZERO),
                        m.totalVat.sum().coalesce(BigDecimal.ZERO),
                        m.totalRefund.sum().coalesce(BigDecimal.ZERO),
                        m.totalSettlement.sum().coalesce(BigDecimal.ZERO)
                )
                .from(m)
                .where(
                        m.userId.eq(userId),
                        m.year.eq(year)
                )
                .fetchOne();

        if (sums == null) {
            return 0;
        }

        BigDecimal totalSales =
                sums.get(m.totalSales.sum().coalesce(BigDecimal.ZERO));
        BigDecimal totalFee =
                sums.get(m.totalFee.sum().coalesce(BigDecimal.ZERO));
        BigDecimal totalVat =
                sums.get(m.totalVat.sum().coalesce(BigDecimal.ZERO));
        BigDecimal totalRefund =
                sums.get(m.totalRefund.sum().coalesce(BigDecimal.ZERO));
        BigDecimal totalSettlement =
                sums.get(m.totalSettlement.sum().coalesce(BigDecimal.ZERO));

        // 2) 기존 yearly row 있는지 확인
        YearlySettlement exists = jpaQueryFactory
                .selectFrom(y)
                .where(
                        y.userId.eq(userId),
                        y.year.eq(year)
                )
                .fetchOne();

        // 3) INSERT
        if (exists == null) {
            YearlySettlement newRow = YearlySettlement.builder()
                    .userId(userId)
                    .year(year)
                    .totalSales(totalSales)
                    .totalFee(totalFee)
                    .totalVat(totalVat)
                    .totalRefund(totalRefund)
                    .totalSettlement(totalSettlement)
                    .build();

            em.persist(newRow);
            return 1;
        }

        // 4) UPDATE
        int result = (int) jpaQueryFactory.update(y)
                .set(y.totalSales, totalSales)
                .set(y.totalFee, totalFee)
                .set(y.totalVat, totalVat)
                .set(y.totalRefund, totalRefund)
                .set(y.totalSettlement, totalSettlement)
                .where(y.yearlyId.eq(exists.getYearlyId()))
                .execute();
        em.flush();
        em.clear();
        return result;
    }
}
