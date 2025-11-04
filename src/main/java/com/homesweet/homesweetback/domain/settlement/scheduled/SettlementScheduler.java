package com.homesweet.homesweetback.domain.settlement.scheduled;

import com.homesweet.homesweetback.domain.auth.entity.UserRole;
import com.homesweet.homesweetback.domain.auth.repository.UserRepository;
import com.homesweet.homesweetback.domain.settlement.repository.DailySettlementRepository;
import com.homesweet.homesweetback.domain.settlement.service.DailySettlementService;
import com.homesweet.homesweetback.domain.settlement.service.WeeklySettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

@RequiredArgsConstructor
@Component
public class SettlementScheduler {
    private final UserRepository userRepository;
    private final DailySettlementService dailySettlementService;
    private final DailySettlementRepository dailySettlementRepository;
    private final WeeklySettlementService weeklySettlementService;

    @Scheduled(cron = "0 */1 * * * *", zone = "Asia/Seoul")
    public void dailyScheduled() {
        ZoneId KST = ZoneId.of("Asia/Seoul");
        LocalDate target = LocalDate.now(KST);
        LocalDateTime start = target.atStartOfDay();
        LocalDateTime endEx = target.plusDays(1).atStartOfDay();

        userRepository.findAllByRole(UserRole.SELLER).forEach(seller -> {
            try {
                dailySettlementService.getSettlement(seller.getId(), start, endEx);
                System.out.println("[Daily-RT] BEFORE userId=" + seller.getId() + " ymd=" + start + " cnt=" + endEx);
            } catch (Exception e) {
                System.out.println("[Daily-RT] FAIL userId=" + seller.getId() + " err=" + e.getMessage());
                e.printStackTrace(System.out);
            }
            System.out.println("일별 스케줄 실행...");
            System.out.println("[Daily-RealTime] sellerId=" + seller.getId() + "date =" + target + "range = " + start + "/endExclusive = " + endEx);
        });
    }

    @Scheduled(cron = "0 */1 * * * *", zone = "Asia/Seoul")
    public void weeklyScheduled() {
        ZoneId KST = ZoneId.of("Asia/Seoul");
        LocalDate target = LocalDate.now(KST);
        LocalDate weekStart = target.minusWeeks(1).with(java.time.DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(7);
        userRepository.findAllByRole(UserRole.SELLER).forEach(seller -> {
            try {
                weeklySettlementService.getWeeklySettlement(seller.getId(), weekStart, weekEnd);
                System.out.println("[Week-RT] BEFORE userId=" + seller.getId() + " ymd=" + weekStart + " cnt=" + weekEnd);
            } catch (Exception e) {
                System.out.println("[Week-RT] FAIL userId=" + seller.getId() + " err=" + e.getMessage());
                e.printStackTrace(System.out);
            }
            System.out.println("주별 스케줄 실행...");
            System.out.println("[Week-RealTime] sellerId=" + seller.getId() + "date =" + target + "range = " + weekStart + "/endExclusive = " + weekEnd);
        });
    }
}
