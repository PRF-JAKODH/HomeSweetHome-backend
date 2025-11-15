package com.homesweet.homesweetback.domain.settlement.util;

import com.homesweet.homesweetback.domain.settlement.dto.response.DailySettlementResponse;
import com.homesweet.homesweetback.domain.settlement.dto.response.WeeklySettlementResponse;
import com.homesweet.homesweetback.domain.settlement.util.calculator.WeeklyDateRangeCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;


import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

//@ExtendWith(MockitoExtension.class)
@DisplayName("EmptyDailyResponse 테스트")
class EmptyResponseTest {
//    @InjectMocks
//    DailySettlementService dailySettlementService;

//    @InjectMocks
    private EmptyResponse emptyResponse;

    @BeforeEach
    void setUp() {
        emptyResponse = new EmptyResponse();
    }

    @Test
    @DisplayName("단일 EmptyDailyResponse 생성 성공")
    void createEmptyDaily() {
        // given
        LocalDate startDate = LocalDate.of(2025, 11, 10);

        // when
        DailySettlementResponse response = emptyResponse.createEmptyDaily(startDate);

        // then
        assertThat(response.totalSales()).isEqualTo(BigDecimal.ZERO);
        assertThat(response.totalFee()).isEqualTo(BigDecimal.ZERO);
        assertThat(response.totalVat()).isEqualTo(BigDecimal.ZERO);
        assertThat(response.totalRefund()).isEqualTo(BigDecimal.ZERO);
        assertThat(response.totalSettlement()).isEqualTo(BigDecimal.ZERO);

        assertThat(response.settlementDate()).isEqualTo(startDate);
        assertThat(response.settlementStatus()).isEqualTo("CANCELED");
        assertThat(response.completedRate()).isEqualTo(0.0);
        assertThat(response.totalCount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("EmptyDailyResponse Page 생성 성공")
    void testCreateEmptyDaily() {
        System.out.println(emptyResponse.getClass());
        System.out.println(emptyResponse.getClass().getName());


        LocalDate startDate = LocalDate.of(2025, 11, 10);
        Pageable pageable = PageRequest.of(0, 10);

        Page<DailySettlementResponse> page =
                emptyResponse.createEmptyDaily(startDate, pageable);

        System.out.println(page);
        System.out.println("TotalElements=" + page.getTotalElements());
        System.out.println("Content=" + page.getContent());
        // 전체 데이터는 0개
        assertThat(page.getTotalElements()).isEqualTo(0L);

        // content 는 placeholder 0개
        assertThat(page.getContent()).hasSize(0);

//        DailySettlementResponse response = page.getContent().get(0);
//
//        assertThat(response.settlementDate()).isEqualTo(startDate);
//        assertThat(response.settlementStatus()).isEqualTo("CANCELED");
    }

    @Test
    @DisplayName("[성공] WeeklyDateRange 기반으로 빈 주별 응답 Page가 생성된다.")
    void createEmptyWeekly_success() {
        // given
        WeeklyDateRangeCalculator.WeeklyDateRange range =
                new WeeklyDateRangeCalculator.WeeklyDateRange(
                        LocalDate.of(2025, 11, 10),
                        LocalDate.of(2025, 11, 17),
                        (byte) 2
                );

        Pageable pageable = PageRequest.of(0, 10);

        Page<WeeklySettlementResponse> result =
                emptyResponse.createEmptyWeekly(range, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(0); // ✔ empty list
        assertThat(result.getTotalElements()).isEqualTo(0);

        // 나머지는 result 자체 값으로만 검증
        assertThat(result.getSize()).isEqualTo(10);
        assertThat(result.getNumber()).isEqualTo(0);
    }


    @Nested
    @DisplayName("실패 케이스")
    class Fail{
        @Test
        @DisplayName("startDate가 null이어도 예외가 발생하지 않는다")
        void createEmptyDaily_nullStartDate_fail() {
            DailySettlementResponse response = emptyResponse.createEmptyDaily(null);
            assertThat(response.settlementDate()).isNull();
        }
        @Test
        @DisplayName("totalElements가 0이 아니면 실패")
        void createEmptyDaily_totalElements_mustBeZero() {
            LocalDate date = LocalDate.of(2025, 11, 10);
            Pageable pageable = PageRequest.of(0, 10);

            Page<DailySettlementResponse> page =
                    emptyResponse.createEmptyDaily(date, pageable);

            assertThat(page.getTotalElements()).isZero();  // 실패해야 정상
        }
        @Test
        @DisplayName("[실패] WeeklyDateRange가 null이면 NullPointerException")
        void createEmptyWeekly_fail_null_range() {
            Pageable pageable = PageRequest.of(0, 10);
            assertThatThrownBy(() ->
                    emptyResponse.createEmptyWeekly(null, pageable)
            ).isInstanceOf(NullPointerException.class);
        }
        @Test
        @DisplayName("[실패] Pageable이 null이면 IllegalArgumentException 발생")
        void createEmptyWeekly_fail_null_pageable() {
            WeeklyDateRangeCalculator.WeeklyDateRange range =
                    new WeeklyDateRangeCalculator.WeeklyDateRange(
                            LocalDate.of(2025, 11, 10),
                            LocalDate.of(2025, 11, 17),
                            (byte) 2
                    );
            assertThatThrownBy(() ->
                    emptyResponse.createEmptyWeekly(range, null)
            ).isInstanceOf(IllegalArgumentException.class)   // ⬅ 수정됨
                    .hasMessageContaining("Pageable must not be null");
        }

        @Test
        @DisplayName("[실패] 빈 페이지여야 하는데 content가 비어있지 않으면 실패")
        void createEmptyWeekly_fail_content_not_empty() {
            WeeklyDateRangeCalculator.WeeklyDateRange range =
                    new WeeklyDateRangeCalculator.WeeklyDateRange(
                            LocalDate.of(2025, 11, 10),
                            LocalDate.of(2025, 11, 17),
                            (byte) 2
                    );
            Pageable pageable = PageRequest.of(0, 10);
            Page<WeeklySettlementResponse> page =
                    emptyResponse.createEmptyWeekly(range, pageable);

            // 실패 조건을 테스트: 비어 있지 않아야 실패
            assertThat(page.getContent())
                    .as("content must be empty")
                    .isEmpty();
        }
    }
}