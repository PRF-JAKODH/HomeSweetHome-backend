package com.homesweet.homesweetback.domain.settlement.service;

import com.homesweet.homesweetback.common.exception.BusinessException;
import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.settlement.util.SettlementStatusUtil;
import com.homesweet.homesweetback.domain.settlement.util.ValidateAndDateRange;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@DisplayName("전체 내역 조회 및 상태별 조회")
public class SettlementStatusListTest {
    @Nested
    @DisplayName("성공 케이스")
    class Success {
        @Test
        @DisplayName("상태값이 all이거나 null일 때 null을 반환한다.")
        void allNull() {
            assertThat(SettlementStatusUtil.normalizeStatus("all")).isNull();
            assertThat(SettlementStatusUtil.normalizeStatus(" ALL ")).isNull();
            assertThat(SettlementStatusUtil.normalizeStatus("All")).isNull();
            assertThat(SettlementStatusUtil.normalizeStatus("ALL")).isNull();
            assertThat(SettlementStatusUtil.normalizeStatus(null)).isNull();
        }

        @Test
        @DisplayName("다른 문자열은 그대로 반환한다.")
        void SettlementStatus() {
            assertThat(SettlementStatusUtil.normalizeStatus("COMPLETED")).isEqualTo("COMPLETED");
            assertThat(SettlementStatusUtil.normalizeStatus("PENDING")).isEqualTo("PENDING");
            assertThat(SettlementStatusUtil.normalizeStatus("CANCELED")).isEqualTo("CANCELED");
        }

        @Test
        @DisplayName("정상 범위 입력시 dateRange 반환한다.")
        void dateRange() {
            LocalDateTime startDate = LocalDateTime.of(2025, Month.JANUARY, 1, 0, 0, 0);
            LocalDateTime endDate = LocalDateTime.of(2025, Month.JANUARY, 5, 4, 15, 0);

            ValidateAndDateRange.DateRange dateRange = ValidateAndDateRange.validateAndDateRange(startDate, endDate);

            assertThat(dateRange.start()).isEqualTo(LocalDate.of(2025, 1, 1).atStartOfDay());
            assertThat(dateRange.end()).isEqualTo(LocalDate.of(2025, 1, 6).atStartOfDay());
        }
    }

    @Nested
    @DisplayName("실패 케이스")
    class Failure {
        @Test
        @DisplayName("시작일이 종료일보다 늦으면 예외 발생")
        void ValidateAndDateRange_Failure() {
            LocalDateTime startDate = LocalDateTime.of(2025, Month.JANUARY, 10, 0, 0, 0);
            LocalDateTime endDate = LocalDateTime.of(2025, Month.JANUARY, 5, 4, 15, 0);

            assertThatThrownBy(() -> ValidateAndDateRange.validateAndDateRange(startDate, endDate))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ErrorCode.INVALID_DATE_RANGE.getMessage());
        }
        @Test
        @DisplayName("시작일이 null이면 예외 발생")
        void ValidateAndDateRange_Failure_StartDateIsNull() {
            assertThatThrownBy(() -> ValidateAndDateRange.validateAndDateRange(null, LocalDateTime.now()))
                    .isInstanceOf(BusinessException.class);
        }
        @Test
        @DisplayName("종료일이 null이면 예외 발생")
        void ValidateAndDateRange_Failure_EndDateIsNull() {
            assertThatThrownBy(() -> ValidateAndDateRange.validateAndDateRange(LocalDateTime.now(), null))
                    .isInstanceOf(BusinessException.class);
        }
    }
}