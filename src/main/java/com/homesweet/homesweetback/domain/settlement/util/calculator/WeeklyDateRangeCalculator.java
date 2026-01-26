package com.homesweet.homesweetback.domain.settlement.util.calculator;

import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;

// 월의 주 구하기
@Component
public class WeeklyDateRangeCalculator {
    public static WeeklyDateRange getWeeklyDateRange(LocalDate startDate, LocalDate endDate) {
        // 1. 이번 달의 첫 번째 월요일을 찾아야 함 (11월 기준: 11/3)
        LocalDate firstMondayOfMonth = startDate.withDayOfMonth(1)
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));

        // 2. startDate가 이 월요일보다 이전이면 → 무조건 첫 월요일을 사용
        LocalDate weekStart;
        if (startDate.isBefore(firstMondayOfMonth)) {
            weekStart = firstMondayOfMonth;
        } else {
            // 그 이후면 그냥 속한 주의 월요일 계산
            weekStart = startDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        }

        // 3. end 기준 다음 주 월요일
        LocalDate lastWeekStartEx = endDate.plusDays(1)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        // 4. 주차 계산: 첫 월요일부터 몇 번째 주인지
        long diff = java.time.temporal.ChronoUnit.DAYS.between(firstMondayOfMonth, weekStart);
        byte week = (byte) (diff / 7 + 1);

        return new WeeklyDateRange(weekStart, lastWeekStartEx, week);
    }

    public record WeeklyDateRange(LocalDate firstWeekStart, LocalDate lastWeekStartEx, byte week) {
    }

    // 주 시작일(월요일)
    public static LocalDate monday(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    // 주 종료일(일요일)
    public static LocalDate sunday(LocalDate date) {
        return date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
    }
}