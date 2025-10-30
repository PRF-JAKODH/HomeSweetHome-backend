package com.homesweet.homesweetback.domain.settlement.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.Locale;

class WeeklySettlementServiceTest {

    @Test
    void getSettlement() {
        LocalDate date = LocalDate.of(2025, 10, 28); // 예: 2025-02-11
        WeekFields weekFields = WeekFields.of(Locale.getDefault());

// 🔥 해당 월 기준 몇 주차인지
        int weekOfMonth = date.get(weekFields.weekOfMonth());
        System.out.println("주차: " + weekOfMonth + "주차");
    }


}