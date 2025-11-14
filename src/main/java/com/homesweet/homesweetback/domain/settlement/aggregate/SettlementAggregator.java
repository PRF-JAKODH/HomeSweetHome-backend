package com.homesweet.homesweetback.domain.settlement.aggregate;

import com.homesweet.homesweetback.domain.settlement.entity.Settlement;
import com.homesweet.homesweetback.domain.settlement.util.calculator.SettlementCalculator;
import com.homesweet.homesweetback.domain.settlement.util.vo.DailyTotals;
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

    public <K> Map<K, DailyTotals> aggregate(
            List<Settlement> settlements,
            Function<Settlement, K> keyExtractor
    ) {
        Map<K, DailyTotals> result = new LinkedHashMap<>();
        K currentKey = null;
        DailyTotals totals = null;
        for (Settlement s : settlements) {
            K key = keyExtractor.apply(s);
            // 새로운 그룹 시작
            if (!key.equals(currentKey)) {
                // 이전 그룹 저장
                if (currentKey != null) {
                    result.put(currentKey, totals);
                }
                // 새로운 그룹 초기화
                currentKey = key;
                totals = DailyTotals.empty();
            }
            // 누적
            settlementCalculator.accumulate(totals, s);
        }
        // 마지막 그룹 저장
        if (currentKey != null) {
            result.put(currentKey, totals);
        }
        return result;
    }
}

