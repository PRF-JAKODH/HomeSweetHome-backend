package com.homesweet.homesweetback.domain.settlement.util;

import com.homesweet.homesweetback.domain.settlement.dto.response.DailySettlementResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
public class DailyResponseFactory {
    public DailySettlementResponse createEmptyDaily(LocalDate startDate) {
        return new DailySettlementResponse(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, startDate, "CANCELED", 0.0, 0L
        );
    }
    public Page<DailySettlementResponse> createEmptyDaily(LocalDate startDate, Pageable pageable) {
        return new PageImpl<>(List.of(createEmptyDaily(startDate)), pageable, 0);
    }
}
