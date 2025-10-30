package com.homesweet.homesweetback.domain.settlement;

import com.homesweet.homesweetback.domain.order.entity.Order;
import com.homesweet.homesweetback.domain.order.repository.OrderRepository;
import com.homesweet.homesweetback.domain.settlement.dto.response.MonthlySettlementResponse;
import com.homesweet.homesweetback.domain.settlement.dto.response.WeeklySettlementResponse;
import com.homesweet.homesweetback.domain.settlement.entity.*;
import com.homesweet.homesweetback.domain.settlement.repository.*;
import com.homesweet.homesweetback.domain.settlement.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class SettlementTest {
    @Autowired
    private DailySettlementService dailySettlementService;
    @Autowired
    private SettlementService settlementService;
    @Autowired
    private SettlementRepository settlementRepository;
    @Autowired
    private DailySettlementRepository dailySettlementRepository;
    @Autowired
    private WeeklySettlementRepository weeklySettlementRepository;
    @Autowired
    private WeeklySettlementService weeklySettlementService;
    @Autowired
    private MonthlySettlementService monthlySettlementService;
    @Autowired
    private MonthlySettlementRepository monthlySettlementRepository;
    @Autowired
    private YearlySettlementService yearlySettlementService;
    @Autowired
    private YearlySettlementRepository yearlySettlementRepository;
    @Autowired
    private OrderRepository orderRepository;


    @Test
    void 일자별_정산_집계_성공() {

        // given
        LocalDateTime startDate = LocalDateTime.of(2025, 10, 29, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2025, 10, 29, 23, 59);

        // when
        dailySettlementService.getSettlement(1L, startDate, endDate);
        System.out.println();

        // then
        // ▶ daily_settlements 테이블에 데이터가 들어갔는지 검증
        List<DailySettlement> results = dailySettlementRepository.findAll();
        assertThat(results).isNotEmpty();

        System.out.println("일별 정산 수 = " + results.size());
    }

    @Test
    void 주차별_정산_집계_성공() {
        // given: DailySettlement 데이터는 이미 DB에 존재한다고 가정

        // when: 주차별 정산 집계 실행
        weeklySettlementService.getWeeklySettlement(1L);

        // then: WeeklySettlement 테이블에 값이 들어왔는지 확인
        List<WeeklySettlement> result = weeklySettlementRepository.findAll();
        System.out.println("주차 정산 결과 = " + result); // 디버깅용 출력

        assertThat(result).isNotEmpty();       // ✅ 결과가 비어있지 않아야 함
        assertThat(result.get(0).getUserId()).isEqualTo(1L); // ✅ 특정 판매자 기준
    }

    @Test
    void 월별_집계_성공() {
        monthlySettlementService.getMonthlySettlement(4L);

        List<MonthlySettlement> result = monthlySettlementRepository.findAll();
        System.out.println("월별 정산 결과 = " + result); // 디버깅용 출력

        assertThat(result).isNotEmpty();       // ✅ 결과가 비어있지 않아야 함
        assertThat(result.get(0).getUserId()).isEqualTo(4L); // ✅ 특정 판매자 기준
    }

    @Test
    void 연별_집계_성공() {
        yearlySettlementService.getYearlySettlement(2L);

        List<YearlySettlement> result = yearlySettlementRepository.findAll();
        System.out.println("연별 정산 결과 = " + result); // 디버깅용 출력

        assertThat(result).isNotEmpty();
        assertThat(result.get(0).getUserId()).isEqualTo(2L); //
    }

    @Test
    void 일별_정산_완료율_계산_성공() {
        Long userId = 1L; // 이미 존재하는 userId 기준
        LocalDate date = LocalDate.now();
        // when
        double completionRate = dailySettlementService.getDailySummary(userId, date);

        // then
        System.out.println("일별 정산 완료율 = " + completionRate + "%");
        assertThat(completionRate).isGreaterThanOrEqualTo(0.0);
    }

    @Test
    void 주별_정산_완료율_계산_성공() {
        Long userId = 1L;
        LocalDate date = LocalDate.now();  // 오늘 날짜 기준

        WeeklySettlementResponse result = weeklySettlementService.getWeeklySummary(userId, date);

        System.out.println("이번 주 정산 완료율 = " + result + "%");

        // ✅ 결과 수동 확인용 (assert 생략 가능, 혹은 예상값이 명확하다면 assert 추가)
        assertTrue(result >= 0 && result <= 100);
    }

    @Test
    void createSettlement() {
        Long orderId = 4L;   // 실제 존재하는 Order PK 사용
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order가 없습니다"));

        settlementService.createSettlement(order);

        List<Settlement> settlements = settlementRepository.findByUserId(order.getUser().getId());
        assertFalse(settlements.isEmpty(), "정산 데이터가 저장되지 않았습니다.");

        Settlement s = settlements.get(0);
        assertEquals(order.getId(), s.getOrder().getId());
        assertEquals("PENDING", s.getSettlementStatus());
        assertNotNull(s.getSettlementDate());
        assertTrue(s.getSettlementAmount().compareTo(BigDecimal.ZERO) > 0);

        System.out.println("Settlement 저장 성공: " + s.getSettlementAmount());
    }

    @Test
    void getDailySettlementStatus() {
        Long userId = 1L;
        LocalDate today = LocalDate.of(2025, 10, 29); // 테스트 기준일

        List<Settlement> settlements = dailySettlementService.getDailySettlementStatus(userId, today, "COMPLETED"); // 상태는 임의로 COMPLETED로 테스트);
        System.out.println("=== 조회된 Settlement 목록 ===");
        settlements.forEach(s -> { System.out.println(
                    "orderId=" + s.getOrder().getId() +
                            ", orderedAt=" + s.getOrder().getOrderedAt() +
                            ", settlementStatus=" + s.getSettlementStatus()
            );
        });
        assertTrue(settlements.size() > 0, "해당 날짜 범위에 정산 데이터가 없습니다.");
    }

    @Test
    void getWeeklySettlementDate() {
        Long userId = 1L;
        LocalDate today = LocalDate.now();

        List<Settlement> settlements = weeklySettlementService.getSettlementStatus(userId, today, "COMPLETED");
        System.out.println("조회된 COMPLETED 정산 건수 = " + settlements.size());
        settlements.forEach(s ->
                System.out.println("orderId=" + s.getOrder().getId() + ", status=" + s.getSettlementStatus())
        );
        assertNotNull(settlements);
        assertTrue(settlements.stream().allMatch(s -> "COMPLETED".equals(s.getSettlementStatus())));
    }

    @Test
    void getMonthlySettlementDate() {
        Long userId = 2L;
        LocalDate today = LocalDate.now();   // 오늘 기준 (예: 2025-01-10)
        List<Settlement> settlements = monthlySettlementService.getMonthlySettlementStatus(userId, today, "COMPLETED");

        System.out.println("이번 달 COMPLETED 정산 건수 = " + settlements.size());
        settlements.forEach(s ->
                System.out.println("orderId=" + s.getOrder().getId() +
                        ", orderedAt=" + s.getOrder().getOrderedAt() +
                        ", status=" + s.getSettlementStatus())
        );

        assertNotNull(settlements);
        assertTrue(settlements.stream().allMatch(s -> "COMPLETED".equals(s.getSettlementStatus())));
    }

    @Test
    void getYearlySettlementDate() {
        Long userId = 1L;
        LocalDate today = LocalDate.now();   // 오늘 기준 (예: 2025-01-10)
        List<Settlement> settlements =
                yearlySettlementService.getYearlySettlementStatus(userId, today, "COMPLETED");

        System.out.println("이번 연도 COMPLETED 정산 건수 = " + settlements.size());
        settlements.forEach(s ->
                System.out.println("orderId=" + s.getOrder().getId() +
                        ", orderedAt=" + s.getOrder().getOrderedAt() +
                        ", status=" + s.getSettlementStatus())
        );

        assertNotNull(settlements);
        assertTrue(settlements.stream().allMatch(s -> "COMPLETED".equals(s.getSettlementStatus())));
    }
    @Test
    void getMonthlyGrowthRate() {
        Long userId = 1L;
        LocalDate date = LocalDate.of(2025, 9, 28); // 아무 기준일

        // when
        MonthlySettlementResponse result =
                monthlySettlementService.getMonthlyGrowthRate(userId, date);

        // then
        System.out.println("===== 테스트 결과 =====");
        System.out.println("Year : " + result.year());
        System.out.println("Month : " + result.month());
        System.out.println("thisMonth Settlement : " + result.totalSettlement());
        System.out.println("growthRate : " + result.growthRate() + "%");

        assertNotNull(result);
        assertNotNull(result.growthRate());

        // 예: 이번달 총정산금 있는지
        assertTrue(result.totalSettlement().compareTo(BigDecimal.ZERO) >= 0);
    }
}