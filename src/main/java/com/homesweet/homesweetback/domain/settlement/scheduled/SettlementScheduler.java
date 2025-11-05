package com.homesweet.homesweetback.domain.settlement.scheduled;

import com.homesweet.homesweetback.domain.auth.entity.UserRole;
import com.homesweet.homesweetback.domain.auth.repository.UserRepository;
import com.homesweet.homesweetback.domain.settlement.repository.DailySettlementRepository;
import com.homesweet.homesweetback.domain.settlement.service.DailySettlementService;
import com.homesweet.homesweetback.domain.settlement.service.MonthlySettlementService;
import com.homesweet.homesweetback.domain.settlement.service.WeeklySettlementService;
import com.homesweet.homesweetback.domain.settlement.service.YearlySettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.cglib.core.Local;
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
    private final MonthlySettlementService monthlySettlementService;
    private final YearlySettlementService yearlySettlementService;

    // 1분마다 실행
    @Scheduled(cron = "0 */1 * * * *", zone = "Asia/Seoul")
    public void dailyScheduled() {
        ZoneId KST = ZoneId.of("Asia/Seoul");
        LocalDate target = LocalDate.now(KST);
        LocalDateTime start = target.atStartOfDay();
        LocalDateTime endEx = target.plusDays(1).atStartOfDay();

        userRepository.findAllByRole(UserRole.SELLER).forEach(seller -> {
            try {
                dailySettlementService.getSettlement(seller.getId(), start, endEx);
            } catch (Exception e) {
                e.printStackTrace(System.out);
            }
            System.out.println("일별 스케줄 실행...");
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
            } catch (Exception e) {
                e.printStackTrace(System.out);
            }
            System.out.println("주별 스케줄 실행...");
        });
    }
    @Scheduled(cron = "0 */1 * * * *", zone = "Asia/Seoul")
    public void monthlyScheduled() {
        ZoneId KST = ZoneId.of("Asia/Seoul");
        LocalDate target = LocalDate.now(KST);
        Short year = (short) target.getYear();
        Byte month = (byte) target.getMonthValue();

        userRepository.findAllByRole(UserRole.SELLER).forEach(seller -> {
            try {
                monthlySettlementService.getMonthlySettlement(seller.getId());
            } catch (Exception e) {
                e.printStackTrace(System.out);
            }
            System.out.println("월별 스케줄 실행...");
        });
    }
    @Scheduled(cron = "0 */1 * * * *", zone = "Asia/Seoul")
    public void yearlyScheduled() {
        ZoneId KST = ZoneId.of("Asia/Seoul");
        LocalDate target = LocalDate.now(KST);
        Short year = (short) target.getYear();

        // 확인용
        LocalDate yearStart = target.withDayOfYear(1);
        LocalDate yearEnd = yearStart.plusYears(1);

        userRepository.findAllByRole(UserRole.SELLER).forEach(seller -> {
            try {
                yearlySettlementService.getYearlySettlement(seller.getId());
            } catch (Exception e) {
                e.printStackTrace(System.out);
            }
            System.out.println("연별 스케줄 실행...");
        });
    }
}
