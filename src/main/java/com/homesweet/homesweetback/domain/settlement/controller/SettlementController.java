package com.homesweet.homesweetback.domain.settlement.controller;

import com.homesweet.homesweetback.domain.settlement.dto.response.DailySettlementResponse;
import com.homesweet.homesweetback.domain.settlement.dto.response.MonthlySettlementResponse;
import com.homesweet.homesweetback.domain.settlement.dto.response.WeeklySettlementResponse;
import com.homesweet.homesweetback.domain.settlement.dto.response.YearlySettlementResponse;
import com.homesweet.homesweetback.domain.settlement.entity.Settlement;
import com.homesweet.homesweetback.domain.settlement.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/settlement")
public class SettlementController {

    private final DailySettlementService dailySettlementService;
    private final MonthlySettlementService monthlySettlementService;
    private final WeeklySettlementService weeklySettlementService;
    private final YearlySettlementService yearlySettlementService;

    @GetMapping("/daily/{userId}")
    public ResponseEntity<List<DailySettlementResponse>> getDailySummary(@PathVariable Long userId, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<DailySettlementResponse> res = dailySettlementService.getDailySummary(userId, date);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/daily/{userId}/status")
    public ResponseEntity<List<Settlement>> getDailyStatus(@PathVariable Long userId, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) String status) {
        List<Settlement> res = dailySettlementService.getDailySettlementStatus(userId, date, status);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/daily/{userId}/generate")
    public ResponseEntity<Void> getDailySettlement(@PathVariable Long userId, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDateTime startDate, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDateTime endDate) {
        dailySettlementService.getSettlement(userId, startDate, endDate);
        return ResponseEntity.ok().build();
    }

    // 2) 특정 년/월에 해당하는 "여러 주" 목록 조회 (월→주 드릴다운용)
//    @GetMapping("/weekly/{userId}/month")
//    public ResponseEntity<List<WeeklySettlementResponse>> getWeeklyListOfMonth(
//            @PathVariable Long userId,
//            @RequestParam int year,
//            @RequestParam int month
//    ) {
//        return ResponseEntity.ok(
//                weeklySettlementService.getWeeklyListOfMonth(userId, year, month)
//        );
//    }


    @GetMapping("/weekly/{userId}")
    public ResponseEntity<List<WeeklySettlementResponse>> getWeeklySummary(@PathVariable Long userId, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<WeeklySettlementResponse> res = weeklySettlementService.getWeeklySummary(userId, date);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/weekly/{userId}/generate")
    public ResponseEntity<Void> getWeeklySettlement(@PathVariable Long userId) {
        weeklySettlementService.getWeeklySettlement(userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/monthly/{userId}")
    public ResponseEntity<MonthlySettlementResponse> getMonthlySummary(@PathVariable Long userId, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        MonthlySettlementResponse res = monthlySettlementService.getMonthlySummary(userId, date);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/monthly/{userId}/generate")
    public ResponseEntity<Void> generateMonthly(@PathVariable Long userId) {
        monthlySettlementService.getMonthlySettlement(userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/yearly/{userId}")
    public ResponseEntity<YearlySettlementResponse> getYearlySummary(@PathVariable Long userId, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        YearlySettlementResponse yearSummary = yearlySettlementService.getYearlySummary(userId, date);
        System.out.println("yearSummary = " + yearSummary);
        return ResponseEntity.ok(yearSummary);
    }

    @PostMapping("/yearly/{userId}/generate")
    public ResponseEntity<Void> getYearlySettlement(@PathVariable Long userId) {
        yearlySettlementService.getYearlySettlement(userId);
        return ResponseEntity.ok().build();
    }
}