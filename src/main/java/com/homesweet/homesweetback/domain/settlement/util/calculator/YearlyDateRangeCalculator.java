package com.homesweet.homesweetback.domain.settlement.util.calculator;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

@Component
public class YearlyDateRangeCalculator {
    public YearlyDateRange calculate(LocalDate startDate, LocalDate endDate) {
        short fromYear = (short) startDate.getYear();
        short toYearEx = (short) (endDate.getYear() + 1);

        LocalDate startYearDate = LocalDate.of(fromYear, 1, 1);
        LocalDate endYearDate = LocalDate.of(toYearEx, 1, 1);

        return new YearlyDateRange(
                YearMonth.of(fromYear, 1),
                fromYear,
                toYearEx,
                startYearDate.atStartOfDay(),
                endYearDate.atStartOfDay()
        );
    }

    public record YearlyDateRange(
            YearMonth fromYearMonth,
            short fromYear,
            short toYearExclusive,
            LocalDateTime fromDateTime,
            LocalDateTime toDateTimeExclusive
    ) {}
}
