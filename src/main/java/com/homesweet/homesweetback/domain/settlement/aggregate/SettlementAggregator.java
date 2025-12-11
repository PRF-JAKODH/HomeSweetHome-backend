package com.homesweet.homesweetback.domain.settlement.aggregate;

import com.homesweet.homesweetback.domain.settlement.util.calculator.SettlementCalculator;
import com.homesweet.homesweetback.domain.settlement.util.vo.SettlementTotals;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

// 집계 공통 로직
@Component
@RequiredArgsConstructor
public class SettlementAggregator {
    private final SettlementCalculator settlementCalculator;
    public <T, K> Map<K, SettlementTotals> aggregate(
            List<T> items,
            Function<T, K> keyExtractor,
            Function<T, SettlementTotals> totalsMapper   // SettlementTotals로 변환
    ) {
        Map<K, SettlementTotals> result = new LinkedHashMap<>();

        for (T item : items) {
            K key = keyExtractor.apply(item);

            // 기존 키에 totals 가져오거나 새로 생성
            SettlementTotals totals = result.computeIfAbsent(key, k -> SettlementTotals.empty());

            // 개별 item → SettlementTotals 변환
            SettlementTotals mapped = totalsMapper.apply(item);
            if (mapped == null) {
                throw new IllegalArgumentException("totalsMapper returned null");
            }
            // SettlementCalculator로 누적
            settlementCalculator.accumulate(totals, mapped);
        }
        return result;
    }
}