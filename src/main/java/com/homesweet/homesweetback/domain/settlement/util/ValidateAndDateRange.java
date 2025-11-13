package com.homesweet.homesweetback.domain.settlement.util;

import com.homesweet.homesweetback.common.exception.BusinessException;
import com.homesweet.homesweetback.common.exception.ErrorCode;

import java.time.LocalDateTime;

public class ValidateAndDateRange {
    private ValidateAndDateRange() {}

    public static DateRange validateAndDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        // 시작일, 종료일 중
        if (startDate == null || endDate == null) {
            throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);
        }
        LocalDateTime start = startDate.toLocalDate().atStartOfDay();
        LocalDateTime end = endDate.toLocalDate().plusDays(1).atStartOfDay();

        // 시작일이 종료일보다 이후일 수 없음
        if (start.isAfter(end)) {
            throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);
        }
        return new DateRange(start, end);
    }
    public record  DateRange(LocalDateTime start, LocalDateTime end) {
    }
}
