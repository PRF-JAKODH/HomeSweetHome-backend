package com.homesweet.homesweetback.domain.settlement.repository.querydsl;

import com.homesweet.homesweetback.domain.settlement.entity.WeeklySettlement;
import com.homesweet.homesweetback.domain.settlement.repository.WeeklySettlementRepository;
import com.homesweet.homesweetback.domain.settlement.repository.querydsl.impl.WeeklySettlementRepositoryImpl;
import com.homesweet.homesweetback.domain.settlement.util.vo.SettlementTotals;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
@ActiveProfiles("test")
@SpringBootTest
@Transactional
//@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD) // 초기화
public class WeeklySettlementRepositoryImplTest {
    @Autowired
    private CustomWeeklySettlementRepository weeklySettlementRepositoryImpl;

    @Autowired
    private WeeklySettlementRepository weeklySettlementRepository;

    @Autowired
    EntityManager em;


    @BeforeEach
    void setup() {
        weeklySettlementRepository.deleteAll();
        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("[성공] upsertWeekly → INSERT 실행 확인")
    void upsertWeekly_insert_success() {
        // given
        Long userId = 11L;
        LocalDate weekStart = LocalDate.of(2025, 1, 6); // 월요일 가정

        SettlementTotals totals = new SettlementTotals(
                BigDecimal.valueOf(100000),
                BigDecimal.valueOf(10000),
                BigDecimal.valueOf(10000),
                BigDecimal.ZERO,
                BigDecimal.valueOf(80000)
        );

        // when
        int result = weeklySettlementRepositoryImpl.upsertWeekly(userId, weekStart, totals);

        // then
        assertThat(result).isEqualTo(1); // INSERT 시 return = 1

        List<WeeklySettlement> list = weeklySettlementRepository.findByWeeklySettlement(userId);
        assertThat(list).hasSize(1);

        WeeklySettlement saved = list.get(0);

        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getWeekStartDate()).isEqualTo(weekStart);
        assertThat(saved.getWeekEndDate()).isEqualTo(weekStart.plusDays(6));
        assertThat(saved.getMonth()).isEqualTo((byte) 1);
        assertThat(saved.getYear()).isEqualTo((short) 2025);
        assertThat(saved.getTotalSales()).isEqualByComparingTo("100000");
        assertThat(saved.getTotalSettlement()).isEqualByComparingTo("80000");
    }

    @Test
    @DisplayName("[성공] upsertWeekly → UPDATE 실행 확인")
    void upsertWeekly_update_success() {
        // given
        Long userId = 11L;
        LocalDate start = LocalDate.of(2025, 2, 3);

        // 1) 기존 row 저장
        WeeklySettlement weekly = WeeklySettlement.builder()
                .userId(userId)
                .year((short) 2025)
                .month((byte) 2)
                .weekStartDate(start)
                .weekEndDate(start.plusDays(6))
                .totalSales(BigDecimal.valueOf(50000))
                .totalFee(BigDecimal.valueOf(5000))
                .totalVat(BigDecimal.valueOf(5000))
                .totalRefund(BigDecimal.ZERO)
                .totalSettlement(BigDecimal.valueOf(40000))
                .build();

        weeklySettlementRepository.save(weekly);


        // 2) UPDATE 할 새로운 값
        SettlementTotals newTotals = new SettlementTotals(
                BigDecimal.valueOf(300000),
                BigDecimal.valueOf(30000),
                BigDecimal.valueOf(30000),
                BigDecimal.ZERO,
                BigDecimal.valueOf(240000)
        );

        // when
        int updated = weeklySettlementRepositoryImpl.upsertWeekly(userId, start, newTotals);
        em.flush();
        em.clear();

        // then
        assertThat(updated).isEqualTo(1);

        List<WeeklySettlement> list = weeklySettlementRepository.findByWeeklySettlement(userId);
        assertThat(list).hasSize(1);

        WeeklySettlement after = list.get(0);

        // ✔ UPDATE 검증
        assertThat(after.getTotalSales()).isEqualByComparingTo("300000");
        assertThat(after.getTotalFee()).isEqualByComparingTo("30000");
        assertThat(after.getTotalVat()).isEqualByComparingTo("30000");
        assertThat(after.getTotalSettlement()).isEqualByComparingTo("240000");

        // ✔ 월/연도/기간 재계산 검증
        assertThat(after.getMonth()).isEqualTo((byte) 2);
        assertThat(after.getYear()).isEqualTo((short) 2025);
        assertThat(after.getWeekEndDate()).isEqualTo(start.plusDays(6));
    }
    // -----------------------------
    // FAILURE CASES
    // -----------------------------
    @Nested
    @DisplayName("실패 케이스")
    class Fail{

        @Test
        @DisplayName("[실패] totals = null → NPE")
        void upsertWeekly_fail_totals_null() {
            assertThatThrownBy(() ->
                    weeklySettlementRepositoryImpl.upsertWeekly(1L, LocalDate.now(), null)
            ).isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("[실패] weekStartDate = null → NPE")
        void upsertWeekly_fail_weekStart_null() {
            assertThatThrownBy(() ->
                    weeklySettlementRepositoryImpl.upsertWeekly(1L, null, SettlementTotals.empty())
            ).isInstanceOf(NullPointerException.class);
        }
    }

//    @Test
//    @DisplayName("[실패] repository 내부 UPDATE 예외 발생 → 그대로 전파")
//    void upsertWeekly_fail_update_exception() {
//        Long userId = 10L;
//        LocalDate start = LocalDate.of(2025, 1, 6);
//
//        // INSERT 하나 넣어둔다
//        weeklySettlementRepository.save(
//                WeeklySettlement.builder()
//                        .userId(userId)
//                        .year((short) 2025)
//                        .month((byte) 1)
//                        .weekStartDate(start)
//                        .weekEndDate(start.plusDays(6))
//                        .totalSales(BigDecimal.valueOf(5000))
//                        .totalFee(BigDecimal.valueOf(500))
//                        .totalVat(BigDecimal.valueOf(500))
//                        .totalRefund(BigDecimal.ZERO)
//                        .totalSettlement(BigDecimal.valueOf(4000))
//                        .build()
//        );
//
//        // totals 내부 특정 필드 null → 정상 (현재 구조)
//        SettlementTotals totals = new SettlementTotals(
//                null,
//                BigDecimal.valueOf(5000),
//                BigDecimal.ZERO,
//                BigDecimal.ONE,
//                BigDecimal.valueOf(2000)
//        );
//
//        // 강제로 예외 발생시키기 위해 존재 row의 userId를 null로 변경 → JPA 오류 유도
//        WeeklySettlement saved = weeklySettlementRepository.findAll().get(0);
//        saved.setUserId(null);
//
//        assertThatThrownBy(() ->
//                weeklyImpl.upsertWeekly(userId, start, totals)
//        ).isInstanceOf(Exception.class);
//    }
}
