package com.homesweet.homesweetback.domain.settlement.util.vo;

import com.homesweet.homesweetback.domain.settlement.entity.DailySettlement;
import com.homesweet.homesweetback.domain.settlement.entity.Settlement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@DisplayName("SettlementTotals 테스트")
class SettlementTotalsTest {

    @Test
    @DisplayName("[성공] Settlement → SettlementTotals 정상 변환")
    void from_success() {
        Settlement s = Settlement.builder()
                .salesAmount(BigDecimal.valueOf(100))
                .fee(BigDecimal.valueOf(10))
                .vat(BigDecimal.valueOf(5))
                .refundAmount(BigDecimal.ZERO)
                .settlementAmount(BigDecimal.valueOf(90))
                .build();

        SettlementTotals totals = SettlementTotals.from(s);

        assertThat(totals.getTotalSales()).isEqualTo(BigDecimal.valueOf(100));
        assertThat(totals.getTotalFee()).isEqualTo(BigDecimal.valueOf(10));
        assertThat(totals.getTotalVat()).isEqualTo(BigDecimal.valueOf(5));
        assertThat(totals.getTotalRefund()).isEqualTo(BigDecimal.ZERO);
        assertThat(totals.getTotalSettlement()).isEqualTo(BigDecimal.valueOf(90));
    }

    @Test
    @DisplayName("[성공] DailySettlement 값을 정상적으로 누적한다")
    void add_success() {

        SettlementTotals totals = SettlementTotals.empty();

        DailySettlement d = DailySettlement.builder()
                .totalSales(BigDecimal.valueOf(100))
                .totalFee(BigDecimal.valueOf(10))
                .totalVat(BigDecimal.valueOf(5))
                .totalRefund(BigDecimal.ZERO)
                .totalSettlement(BigDecimal.valueOf(85))
                .build();

        totals.add(d);

        assertThat(totals.getTotalSales()).isEqualTo(BigDecimal.valueOf(100));
        assertThat(totals.getTotalFee()).isEqualTo(BigDecimal.valueOf(10));
        assertThat(totals.getTotalVat()).isEqualTo(BigDecimal.valueOf(5));
        assertThat(totals.getTotalRefund()).isEqualTo(BigDecimal.ZERO);
        assertThat(totals.getTotalSettlement()).isEqualTo(BigDecimal.valueOf(85));
    }

    @Test
    @DisplayName("[성공] DailySettlement 내부 null 필드는 ZERO 로 처리하여 누적한다")
    void add_success_null_fields_safe() {

        SettlementTotals totals = SettlementTotals.empty();

        DailySettlement d = DailySettlement.builder()
                .totalSales(null)
                .totalFee(null)
                .totalVat(null)
                .totalRefund(null)
                .totalSettlement(null)
                .build();

        totals.add(d);

        assertThat(totals.getTotalSales()).isEqualTo(BigDecimal.ZERO);
        assertThat(totals.getTotalFee()).isEqualTo(BigDecimal.ZERO);
        assertThat(totals.getTotalVat()).isEqualTo(BigDecimal.ZERO);
        assertThat(totals.getTotalRefund()).isEqualTo(BigDecimal.ZERO);
        assertThat(totals.getTotalSettlement()).isEqualTo(BigDecimal.ZERO);
    }


    @Test
    @DisplayName("[실패] Settlement 자체가 null이면 NPE 발생")
    void from_fail_nullSettlement() {
        assertThatThrownBy(() ->
                SettlementTotals.from(null)
        ).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("[실패] add(DailySettlement null) 호출 시 NPE 발생")
    void add_fail_null_dailySettlement() {
        SettlementTotals totals = SettlementTotals.empty();

        assertThatThrownBy(() ->
                totals.add((DailySettlement) null)   // ← 명시적 캐스팅
        ).isInstanceOf(NullPointerException.class);
    }

}
