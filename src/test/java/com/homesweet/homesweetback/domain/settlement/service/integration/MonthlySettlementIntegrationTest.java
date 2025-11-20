package com.homesweet.homesweetback.domain.settlement.service.integration;

import com.homesweet.homesweetback.common.exception.BusinessException;
import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.settlement.data.HelpIntegrationData;
import com.homesweet.homesweetback.domain.settlement.dto.response.MonthlySettlementResponse;
import com.homesweet.homesweetback.domain.settlement.entity.MonthlySettlement;
import com.homesweet.homesweetback.domain.settlement.repository.MonthlySettlementRepository;
import com.homesweet.homesweetback.domain.settlement.repository.WeeklySettlementRepository;
import com.homesweet.homesweetback.domain.settlement.service.MonthlySettlementService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("MonthlySettlementService 통합 테스트")
public class MonthlySettlementIntegrationTest {

    @Autowired
    private MonthlySettlementService monthlyService;

    @Autowired
    private WeeklySettlementRepository weeklyRepository;

    @Autowired
    private MonthlySettlementRepository monthlyRepository;

    @Autowired
    private HelpIntegrationData helper;

    @PersistenceContext
    private EntityManager em;

    private final Long userId = 11L;
    @Nested
    @DisplayName("성공 케이스")
    class Success {

        @Test
        @DisplayName("weekly → monthly INSERT 성공")
        void insertMonthlySuccess() {

            weeklyRepository.save(helper.weekly(
                    userId, (short) 2025, (byte) 11, (byte) 1,
                    new BigDecimal("50000"),
                    new BigDecimal("5000"),
                    new BigDecimal("5000"),
                    BigDecimal.ZERO,
                    new BigDecimal("40000")
            ));

            weeklyRepository.save(helper.weekly(
                    userId, (short) 2025, (byte) 11, (byte) 2,
                    new BigDecimal("30000"),
                    new BigDecimal("3000"),
                    new BigDecimal("3000"),
                    BigDecimal.ZERO,
                    new BigDecimal("24000")
            ));

            em.flush();
            em.clear();
            // when
            monthlyService.getMonthlySettlement(userId);

            // then
            MonthlySettlement saved = em.createQuery(
                            "SELECT m FROM MonthlySettlement m " +
                                    "WHERE m.userId = :userId AND m.year = :year AND m.month = :month",
                            MonthlySettlement.class
                    )
                    .setParameter("userId", userId)
                    .setParameter("year", (short) 2025)
                    .setParameter("month", (byte) 11)
                    .getSingleResult();

            assertThat(saved.getTotalSales()).isEqualByComparingTo("80000");
            assertThat(saved.getTotalFee()).isEqualByComparingTo("8000");
        }
        @Test
        @DisplayName("기존 monthly 존재 시 UPDATE 성공")
        void updateMonthlySuccess() {
            // 기존 row 저장
            MonthlySettlement before = MonthlySettlement.builder()
                    .userId(userId)
                    .year((short) 2025)
                    .month((byte) 11)
                    .totalSales(new BigDecimal("1000"))
                    .totalFee(new BigDecimal("100"))
                    .totalVat(new BigDecimal("100"))
                    .totalRefund(BigDecimal.ZERO)
                    .totalSettlement(new BigDecimal("800"))
                    .build();
            monthlyRepository.save(before);

            // weekly 하나 저장
            weeklyRepository.save(helper.weekly(
                    userId, (short) 2025, (byte) 11, (byte) 1,
                    new BigDecimal("20000"),
                    new BigDecimal("2000"),
                    new BigDecimal("2000"),
                    BigDecimal.ZERO,
                    new BigDecimal("16000")
            ));

            em.flush();
            em.clear();
            // when
            monthlyService.getMonthlySettlement(userId);

            MonthlySettlement updated = em.createQuery(
                            "SELECT m FROM MonthlySettlement m " +
                                    "WHERE m.userId = :userId AND m.year = :year AND m.month = :month",
                            MonthlySettlement.class
                    )
                    .setParameter("userId", userId)
                    .setParameter("year", (short) 2025)
                    .setParameter("month", (byte) 11)
                    .getSingleResult();

            assertThat(updated.getTotalSales()).isEqualByComparingTo("20000");
            assertThat(updated.getTotalFee()).isEqualByComparingTo("2000");
        }

        @Test
        @DisplayName("월별 요약 조회 성공")
        void getMonthlySummarySuccess() {
            MonthlySettlement m = MonthlySettlement.builder()
                    .userId(userId)
                    .year((short) 2025)
                    .month((byte) 11)
                    .totalSales(new BigDecimal("90000"))
                    .totalFee(new BigDecimal("9000"))
                    .totalVat(new BigDecimal("9000"))
                    .totalRefund(BigDecimal.ZERO)
                    .totalSettlement(new BigDecimal("72000"))
                    .build();

            monthlyRepository.save(m);

            em.flush();
            em.clear();

            Page<MonthlySettlementResponse> res =
                    monthlyService.getMonthlySummary(
                            userId,
                            LocalDate.of(2025, 11, 1),
                            LocalDate.of(2025, 11, 30),
                            PageRequest.of(0, 10)
                    );

            assertThat(res.getContent()).hasSize(1);
            assertThat(res.getContent().get(0).totalSales())
                    .isEqualByComparingTo("90000");
        }
        @Test
        @DisplayName("월별 요약 데이터 없으면 empty row 반환")
        void getMonthlySummaryEmpty() {

            Page<MonthlySettlementResponse> res =
                    monthlyService.getMonthlySummary(
                            userId,
                            LocalDate.of(2025, 10, 1),
                            LocalDate.of(2025, 10, 31),
                            PageRequest.of(0, 10)
                    );

            assertThat(res.getContent()).hasSize(1);
            assertThat(res.getContent().get(0).totalSales())
                    .isEqualByComparingTo("0");
        }
    }
    @Nested
    @DisplayName("실패 케이스")
    class Fail {
        @Test
        @DisplayName("weekly 데이터 없으면 BusinessException 발생")
        void fail_no_weekly_data() {

            assertThatThrownBy(() ->
                    monthlyService.getMonthlySettlement(userId)
            ).isInstanceOf(BusinessException.class)
                    .hasMessage(ErrorCode.SETTLEMENT_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("saveMonthly 중 예외 발생 → 전체 실패해야 함")
        void fail_saveMonthly_error() {
            // 1) 잘못된 데이터를 넣어서 PK 충돌 유발
            MonthlySettlement dup = MonthlySettlement.builder()
                    .userId(userId)
                    .year((short) 2025)
                    .month((byte) 11)
                    .totalSales(new BigDecimal("10000"))
                    .totalFee(new BigDecimal("1000"))
                    .totalVat(new BigDecimal("1000"))
                    .totalRefund(BigDecimal.ZERO)
                    .totalSettlement(new BigDecimal("8000"))
                    .build();

            monthlyRepository.save(dup);

            // 동일 PK intentionally 만들기
            MonthlySettlement dup2 = MonthlySettlement.builder()
                    .userId(userId)
                    .year((short) 2025)
                    .month((byte) 11)
                    .totalSales(new BigDecimal("20000"))
                    .totalFee(new BigDecimal("2000"))
                    .totalVat(new BigDecimal("2000"))
                    .totalRefund(BigDecimal.ZERO)
                    .totalSettlement(new BigDecimal("16000"))
                    .build();

            monthlyRepository.save(dup2); // unique 충돌 유도

            em.flush();
            em.clear();

            assertThatThrownBy(() -> monthlyService.getMonthlySettlement(userId))
                    .isInstanceOf(Exception.class);
        }
    }
}