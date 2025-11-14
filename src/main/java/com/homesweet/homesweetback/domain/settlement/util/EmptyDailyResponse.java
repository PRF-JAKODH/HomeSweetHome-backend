package com.homesweet.homesweetback.domain.settlement.util;

import com.homesweet.homesweetback.domain.settlement.dto.response.DailySettlementResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;


// 빈 일별 응답
@Component
public class EmptyDailyResponse {
    public DailySettlementResponse createEmptyDaily(LocalDate startDate) {
        return new DailySettlementResponse(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, startDate, "CANCELED", 0.0, 0L
        );
    }
    public Page<DailySettlementResponse> createEmptyDaily(LocalDate startDate, Pageable pageable) {
        return new PageImpl<>(Collections.emptyList(), pageable, 0);
    }
}
