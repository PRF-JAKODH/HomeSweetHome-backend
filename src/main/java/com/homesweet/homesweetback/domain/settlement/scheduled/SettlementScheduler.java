package com.homesweet.homesweetback.domain.settlement.scheduled;

import com.homesweet.homesweetback.common.exception.BusinessException;
import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.entity.UserRole;
import com.homesweet.homesweetback.domain.auth.repository.UserRepository;
import com.homesweet.homesweetback.domain.order.entity.DeliveryStatus;
import com.homesweet.homesweetback.domain.order.entity.Order;
import com.homesweet.homesweetback.domain.order.entity.OrderStatus;
import com.homesweet.homesweetback.domain.order.repository.OrderRepository;
import com.homesweet.homesweetback.domain.settlement.repository.DailySettlementRepository;
import com.homesweet.homesweetback.domain.settlement.repository.SettlementRepository;
import com.homesweet.homesweetback.domain.settlement.service.*;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.cglib.core.Local;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@RequiredArgsConstructor
@Component
public class SettlementScheduler {
    private static final Logger log = LogManager.getLogger(SettlementScheduler.class);
    private final UserRepository userRepository;
    private final DailySettlementService dailySettlementService;
    private final WeeklySettlementService weeklySettlementService;
    private final MonthlySettlementService monthlySettlementService;
    private final YearlySettlementService yearlySettlementService;
    private final SettlementService settlementService;
    private final OrderRepository orderRepository;
    private final SettlementRepository settlementRepository;

    // 정산 생성
    @Transactional
    @Scheduled(cron = "0 */1 * * * *", zone = "Asia/Seoul")
    public void settlementScheduled(){
        ZoneId KST = ZoneId.of("Asia/Seoul");
//        LocalDateTime cutoffTime = target.minusDays(1).withHour(23).withMinute(59).withSecond(59);
        LocalDateTime cutoffTime = LocalDateTime.now(KST);
        log.info("정산 생성 실행");
        List<Order> settlementCompleted = settlementRepository.findUnSettlementOrders(OrderStatus.COMPLETED, cutoffTime);
//        if (settlementCompleted.isEmpty()) {
//            throw new BusinessException(ErrorCode.NEW_ORDERS_NOT_FOUND);
//        }
        for (Order order : settlementCompleted) {
            try {
                settlementService.createSettlement(order);
                log.info("정산 생성 완료");
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }
        log.info("정산 생성 스케줄러");
    }

    // 정산 취소
    @Transactional
    @Scheduled(cron = "0 */1 * * * *", zone = "Asia/Seoul")
    public void cancelSettlementScheduled(){
        ZoneId KST = ZoneId.of("Asia/Seoul");
        LocalDateTime target = LocalDateTime.now(KST);
//        LocalDateTime cutoffTime = target.minusHours(1);
        LocalDateTime cutoffTime = LocalDateTime.now(KST);
        log.info("정산 취소 실행");
        List<Order> canceledOrders = settlementRepository.findCancelSettlement(DeliveryStatus.CANCELLED, cutoffTime);
        for (Order order : canceledOrders) {
            try {
                settlementService.orderCanceled(order);
                log.info("정산 취소 완료");
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }
        log.info("정산 취소 스케줄러 완료");
    }

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
            log.info("일별 스케줄 실행");
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
            log.info("주별 스케줄 실행");
        });
    }
    @Scheduled(cron = "0 */1 * * * *", zone = "Asia/Seoul")
    public void monthlyScheduled() {
        ZoneId KST = ZoneId.of("Asia/Seoul");
        // 로그 확인용
//        LocalDate target = LocalDate.now(KST);
//        Short year = (short) target.getYear();
//        Byte month = (byte) target.getMonthValue();

        userRepository.findAllByRole(UserRole.SELLER).forEach(seller -> {
            try {
                monthlySettlementService.getMonthlySettlement(seller.getId());
            } catch (Exception e) {
                e.printStackTrace(System.out);
            }
            log.info("월별 스케줄 실행");
        });
    }
    @Scheduled(cron = "0 */1 * * * *", zone = "Asia/Seoul")
    public void yearlyScheduled() {
        ZoneId KST = ZoneId.of("Asia/Seoul");
        LocalDate target = LocalDate.now(KST);
        Short year = (short) target.getYear();

        // 로그 확인용
//        LocalDate yearStart = target.withDayOfYear(1);
//        LocalDate yearEnd = yearStart.plusYears(1);

        userRepository.findAllByRole(UserRole.SELLER).forEach(seller -> {
            try {
                yearlySettlementService.getYearlySettlement(seller.getId());
            } catch (Exception e) {
                e.printStackTrace(System.out);
            }
            log.info("연별 스케줄 실행");
        });
    }
}
