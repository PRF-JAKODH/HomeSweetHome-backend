package com.homesweet.homesweetback.domain.settlement.repository.querydsl;

import com.homesweet.homesweetback.domain.settlement.entity.DailySettlement;
import com.homesweet.homesweetback.domain.settlement.repository.DailySettlementRepository;
import com.homesweet.homesweetback.domain.settlement.repository.querydsl.impl.DailySettlementRepositoryImpl;
import com.homesweet.homesweetback.domain.settlement.util.vo.SettlementTotals;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
public class DailySettlementRepositoryImplTest {
    @Autowired
    private DailySettlementRepository dailySettlementRepository;

    @Autowired
    private CustomDailySettlementRepository customDailySettlementRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setup() {
        dailySettlementRepository.deleteAll();
    }


    @Test
    @DisplayName("[성공] upsertDaily → INSERT 동작 확인")
    void upsertDaily_insert_success() {
        // given
        Long userId = 11L;
        LocalDateTime date = LocalDateTime.of(2025, 1, 10, 0, 0);

        SettlementTotals totals = new SettlementTotals(
                BigDecimal.valueOf(10000),
                BigDecimal.valueOf(1000),
                BigDecimal.valueOf(1000),
                BigDecimal.ZERO,
                BigDecimal.valueOf(9000)
        );

        // when
        customDailySettlementRepository.upsertDaily(userId, date, totals);

        // then
        // 네가 보내준 Repo에 존재하는 findByDailySettlement 로 조회
        List<DailySettlement> savedList = dailySettlementRepository.findByDailySettlement(userId);
        assertThat(savedList).hasSize(1);

        DailySettlement saved = savedList.get(0);
        assertThat(saved.getSettlementDate()).isEqualTo(date);
        assertThat(saved.getTotalSales()).isEqualByComparingTo("10000");
        assertThat(saved.getTotalSettlement()).isEqualByComparingTo("9000");
    }

    @Test
    @DisplayName("[성공] upsertDaily → UPDATE 동작 확인")
    void upsertDaily_update_success() {
        // given
        Long userId = 11L;
        LocalDateTime date = LocalDateTime.of(2025, 1, 10, 0, 0).withNano(0);
        LocalDateTime normalized = date.withHour(0).withMinute(0).withSecond(0).withNano(0);

        // 기존 데이터 INSERT
        DailySettlement original = DailySettlement.builder()
                .userId(userId)
                .settlementDate(normalized)
                .totalSales(BigDecimal.valueOf(5000))
                .totalFee(BigDecimal.valueOf(500))
                .totalVat(BigDecimal.valueOf(500))
                .totalRefund(BigDecimal.ZERO)
                .totalSettlement(BigDecimal.valueOf(4000))
                .build();

        dailySettlementRepository.save(original);

        // UPDATE 값
        SettlementTotals updatedTotals = new SettlementTotals(
                BigDecimal.valueOf(20000),
                BigDecimal.valueOf(2000),
                BigDecimal.valueOf(2000),
                BigDecimal.ZERO,
                BigDecimal.valueOf(16000)
        );

        // when
        customDailySettlementRepository.upsertDaily(userId, date, updatedTotals);

        // then
        List<DailySettlement> results = dailySettlementRepository.findByDailySettlement(userId);
        assertThat(results).hasSize(1);

        DailySettlement updated = results.get(0);

        assertThat(updated.getTotalSales()).isEqualByComparingTo("20000");
        assertThat(updated.getTotalFee()).isEqualByComparingTo("2000");
        assertThat(updated.getTotalVat()).isEqualByComparingTo("2000");
        assertThat(updated.getTotalSettlement()).isEqualByComparingTo("16000");
    }
    @Nested
    @DisplayName("실패 케이스")
    class Fail{
        @Test
        @DisplayName("[실패] settlementDate = null → NPE")
        void fail_null_settlementDate() {

            Long userId = 11L;
            SettlementTotals totals = new SettlementTotals(
                    BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN
            );

            assertThatThrownBy(() ->
                    customDailySettlementRepository.upsertDaily(userId, null, totals)
            ).isInstanceOf(NullPointerException.class);
        }

        // -----------------------------------------------------
        // 실패 케이스 2 : totals = null
        // -----------------------------------------------------
        @Test
        @DisplayName("totals = null → NPE")
        void fail_null_totals() {

            Long userId = 11L;
            LocalDateTime date = LocalDateTime.now();

            assertThatThrownBy(() ->
                    customDailySettlementRepository.upsertDaily(userId, date, null)
            ).isInstanceOf(NullPointerException.class);
        }

        // -----------------------------------------------------
        // 실패 케이스 3 : totals 내부 일부 null → 정상 동작해야 함
        // -----------------------------------------------------
        @Test
        @DisplayName("totals 내부 일부 null → DB에 null 저장")
        void success_partial_null_fields() {

            Long userId = 11L;
            LocalDateTime date = LocalDateTime.of(2025,1,10,0,0);

            SettlementTotals totals = new SettlementTotals(
                    null,
                    BigDecimal.valueOf(1000),
                    null,
                    BigDecimal.ZERO,
                    BigDecimal.valueOf(9000)
            );

            customDailySettlementRepository.upsertDaily(userId, date, totals);

            DailySettlement saved = dailySettlementRepository.findByDailySettlement(userId).get(0);
            assertThat(saved.getTotalSales()).isNull();
            assertThat(saved.getTotalVat()).isNull();
        }

        // -----------------------------------------------------
        // 실패 케이스 4 : userId 존재 X → INSERT 됨 (예외 없음)
        // -----------------------------------------------------
        @Test
        @DisplayName("[실패] 존재하지 않는 userId → FK 예외 발생")
        void fail_unknown_user_inserted_fk_violation() {

            Long userId = 999L;
            LocalDateTime date = LocalDateTime.of(2025,1,10,0,0);

            SettlementTotals totals = new SettlementTotals(
                    BigDecimal.valueOf(1000),
                    BigDecimal.valueOf(100),
                    BigDecimal.valueOf(100),
                    BigDecimal.ZERO,
                    BigDecimal.valueOf(900)
            );

            // 1. FK 예외 발생 확인
            assertThatThrownBy(() ->
                    customDailySettlementRepository.upsertDaily(userId, date, totals)
            ).isInstanceOf(DataIntegrityViolationException.class);

            // 2. [해결책 2] 예외 발생으로 오염된 세션을 클리어하여 AssertionFailure를 방지
            entityManager.clear();

            // 3. 데이터가 삽입되지 않았는지 확인
            assertThat(dailySettlementRepository.findByDailySettlement(userId)).isEmpty();
        }

        // -----------------------------------------------------
        // 실패 케이스 5 : UPDATE 조건 mismatch → 업데이트 안돼야 함
        // -----------------------------------------------------
        @Test
        @DisplayName("[실패] UPDATE 조건 불일치 → updateCount = 0")
        void fail_update_condition_not_match() {

            Long userId = 11L;
            LocalDateTime date = LocalDateTime.of(2025,1,10,0,0);

            // INSERT
            dailySettlementRepository.save(DailySettlement.builder()
                    .userId(userId)
                    .settlementDate(date)
                    .totalSales(BigDecimal.valueOf(1000))
                    .totalSettlement(BigDecimal.valueOf(900))
                    .build());

            // 다른 날짜로 UPDATE 시도 → 조건 불일치
            SettlementTotals totals = new SettlementTotals(
                    BigDecimal.valueOf(9999),
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.valueOf(9999)
            );

            customDailySettlementRepository.upsertDaily(userId, date.plusDays(1), totals);

            DailySettlement saved = dailySettlementRepository.findByDailySettlement(userId).get(0);

            // 값이 업데이트 되지 않아야 한다
            assertThat(saved.getTotalSales()).isEqualByComparingTo("1000");
        }

        // -----------------------------------------------------
        // 실패 케이스 6 : UPDATE인데 INSERT 두 번 되면 안됨
        // -----------------------------------------------------
        @Test
        @DisplayName("동일 row UPDATE인데 INSERT 발생하면 안 됨")
        void fail_duplicate_insert_on_update() {

            Long userId = 11L;
            LocalDateTime date = LocalDateTime.of(2025,1,10,0,0);

            // INSERT
            dailySettlementRepository.save(DailySettlement.builder()
                    .userId(userId)
                    .settlementDate(date)
                    .totalSales(BigDecimal.valueOf(1000))
                    .totalSettlement(BigDecimal.valueOf(900))
                    .build());

            SettlementTotals totals = new SettlementTotals(
                    BigDecimal.valueOf(2000),
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.valueOf(2000)
            );

            // UPDATE 실행
            customDailySettlementRepository.upsertDaily(userId, date, totals);

            // row 수는 반드시 1이어야 한다
            assertThat(dailySettlementRepository.findByDailySettlement(userId)).hasSize(1);
        }
    }
}
