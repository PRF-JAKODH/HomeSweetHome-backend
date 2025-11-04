package com.homesweet.homesweetback.domain.settlement.scheduled;

import com.homesweet.homesweetback.domain.auth.entity.UserRole;
import com.homesweet.homesweetback.domain.auth.repository.UserRepository;
import com.homesweet.homesweetback.domain.settlement.repository.DailySettlementRepository;
import com.homesweet.homesweetback.domain.settlement.service.DailySettlementService;
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

    @Scheduled(cron = "0 */1 * * * *", zone = "Asia/Seoul")
    public void dailyScheduled() {
        ZoneId KST = ZoneId.of("Asia/Seoul");
        LocalDate target = LocalDate.now(KST);
        LocalDateTime start = target.atStartOfDay();
        LocalDateTime endEx = target.plusDays(1).atStartOfDay();

        userRepository.findAllByRole(UserRole.SELLER).forEach(seller -> {
//            try {
//                dailySettlementService.getSettlement(seller.getId(), startDate, endExclusive);
//                System.out.println("[Daily-RT] BEFORE userId=" + seller.getId() + " ymd=" + startDate + " cnt=" + endExclusive);
//            } catch (Exception e) {
//                System.out.println("[Daily-RT] FAIL userId=" + seller.getId() + " err=" + e.getMessage());
//                e.printStackTrace(System.out);
//            }
//            System.out.println("일별 스케줄 실행...");
//            System.out.println("[Daily-RealTime] sellerId=" + seller.getId() +  "date =" + target + "range = " + startDate + "/endExclusive = " + endExclusive);
            try {
                int before = dailySettlementRepository
                        .countByUserIdAndSettlementDateRange(seller.getId(), start, endEx);
                System.out.println("[Daily-RT] BEFORE userId=" + seller.getId()
                        + " range=[" + start + " ~ " + endEx + ") cnt=" + before);

                dailySettlementService.getSettlement(seller.getId(), start, endEx);

                int after = dailySettlementRepository
                        .countByUserIdAndSettlementDateRange(seller.getId(), start, endEx);
                System.out.println("[Daily-RT]  AFTER userId=" + seller.getId()
                        + " range=[" + start + " ~ " + endEx + ") cnt=" + after
                        + " delta=" + (after - before));
            } catch (Exception e) {
                System.out.println("[Daily-RT] FAIL userId=" + seller.getId() + " err=" + e.getMessage());
                e.printStackTrace(System.out);
            }
        });
    }
//    @Scheduled
//    public void weeklyScheduled() {
//        ZoneId KST = ZoneId.of("Asia/Seoul");
////        LocalDate
//    }

}
