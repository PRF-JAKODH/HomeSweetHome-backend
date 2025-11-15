package com.homesweet.homesweetback.domain.settlement.aggregate;

import com.homesweet.homesweetback.domain.settlement.util.calculator.SettlementCalculator;
import com.homesweet.homesweetback.domain.settlement.util.vo.SettlementTotals;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("정산 집계 공통 로직 테스트")
class SettlementAggregatorTest {
    @Mock
    SettlementCalculator settlementCalculator;

    @InjectMocks
    SettlementAggregator settlementAggregator;

    @Test
    @DisplayName("[성공] 서로 다른 Key이면 Map에 두 개가 담긴다.")
    void aggregate_success_different_key() {
        // given
        List<String> items = List.of("A", "B");

        SettlementTotals totalsA = new SettlementTotals(BigDecimal.valueOf(100), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        SettlementTotals totalsB = new SettlementTotals(BigDecimal.valueOf(200), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        willAnswer(invocation -> {
            SettlementTotals acc = invocation.getArgument(0);
            SettlementTotals add = invocation.getArgument(1);
            acc.add(add);
            return null;
        }).given(settlementCalculator).accumulate(any(), any());
        // when
        Map<String, SettlementTotals> result = settlementAggregator.aggregate(
                items,
                item -> item,
                item -> item.equals("A") ? totalsA : totalsB
        );

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get("A").getTotalSales()).isEqualTo(BigDecimal.valueOf(100));
        assertThat(result.get("B").getTotalSales()).isEqualTo(BigDecimal.valueOf(200));

        verify(settlementCalculator, times(2)).accumulate(any(), any());
    }
    @Test
    @DisplayName("[실패] items가 null이면 NullPointerException 발생")
    void aggregate_fail_items_null() {
        // when & then
        assertThatThrownBy(() ->
                settlementAggregator.aggregate(
                        null,
                        item -> "KEY",
                        item -> SettlementTotals.empty()
                )
        ).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("[실패] keyExtractor null이면 NullPointerException 발생")
    void aggregate_fail_keyExtractor_null() {
        // given
        List<String> items = List.of("A");

        // when & then
        assertThatThrownBy(() ->
                settlementAggregator.aggregate(
                        items,
                        null,
                        item -> SettlementTotals.empty()
                )
        ).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("[실패] totalsMapper null이면 NullPointerException 발생")
    void aggregate_fail_totalsMapper_null() {
        // given
        List<String> items = List.of("A");

        // when & then
        assertThatThrownBy(() ->
                settlementAggregator.aggregate(
                        items,
                        item -> item,
                        null
                )
        ).isInstanceOf(NullPointerException.class);
    }
    @Test
    @DisplayName("[실패] totalsMapper가 null을 반환하면 NullPointerException 발생")
    void aggregate_fail_mapper_returns_null() {
        // given
        List<String> items = List.of("A");

        // when & then
        assertThatThrownBy(() ->
                settlementAggregator.aggregate(
                        items,
                        item -> item,
                        item -> null
                )
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("totalsMapper returned null");
    }




}