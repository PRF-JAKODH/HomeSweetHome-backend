package com.homesweet.homesweetback.domain.settlement.util.calculator;

import org.springframework.cglib.core.Local;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

@Component
public class MonthlyDateRangeCalculator {
    public MonthlyDateRange MonthlyDateRangeCalculate(LocalDate startDate, LocalDate endDate) {
        YearMonth fromYM = YearMonth.from(startDate);
        YearMonth toYM = YearMonth.from(endDate);

        LocalDate fromInclusive = fromYM.atDay(1);                // 해당 월 1일 00:00:00
        LocalDate toExclusive = toYM.plusMonths(1).atDay(1);    // 다음 달 1일 00:00:00

        LocalDateTime from = fromInclusive.atStartOfDay(); // 00:00:00
        LocalDateTime toEx = toExclusive.atStartOfDay();

        short fromYear = (short) fromYM.getYear();
        byte fromMonth = (byte) fromYM.getMonthValue();
        short toYear = (short) toYM.getYear();
        byte toMonth = (byte) toYM.getMonthValue();
        return new MonthlyDateRange(
                fromYM,
                toYM,
                from,
                toEx,
                fromYear,
                fromMonth,
                toYear,
                toMonth
        );
    }
    public record MonthlyDateRange(
            YearMonth fromYM,
            YearMonth toYM,
            LocalDateTime from,
            LocalDateTime toExclusive,
            short fromYear,
            byte fromMonth,
            short toYear,
            byte toMonth
    ) {}
}
