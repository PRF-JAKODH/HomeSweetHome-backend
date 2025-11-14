package com.homesweet.homesweetback.domain.settlement.util;

import com.homesweet.homesweetback.domain.settlement.dto.response.DailySettlementResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;


import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

//@ExtendWith(MockitoExtension.class)
@DisplayName("EmptyDailyResponse 테스트")
class EmptyDailyResponseTest {
//    @InjectMocks
//    DailySettlementService dailySettlementService;

//    @InjectMocks
    private EmptyDailyResponse emptyDailyResponse;

    @BeforeEach
    void setUp() {
        emptyDailyResponse = new EmptyDailyResponse();
    }

    @Test
    @DisplayName("단일 EmptyDailyResponse 생성 성공")
    void createEmptyDaily() {
        // given
        LocalDate startDate = LocalDate.of(2025, 11, 10);

        // when
        DailySettlementResponse response = emptyDailyResponse.createEmptyDaily(startDate);

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
        System.out.println(emptyDailyResponse.getClass());
        System.out.println(emptyDailyResponse.getClass().getName());


        LocalDate startDate = LocalDate.of(2025, 11, 10);
        Pageable pageable = PageRequest.of(0, 10);

        Page<DailySettlementResponse> page =
                emptyDailyResponse.createEmptyDaily(startDate, pageable);

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

    @Nested
    @DisplayName("실패 케이스")
    class Fail{
        @Test
        @DisplayName("startDate가 null이어도 예외가 발생하지 않는다")
        void createEmptyDaily_nullStartDate_fail() {
            DailySettlementResponse response = emptyDailyResponse.createEmptyDaily(null);
            assertThat(response.settlementDate()).isNull();
        }
        @Test
        @DisplayName("totalElements가 0이 아니면 실패")
        void createEmptyDaily_totalElements_mustBeZero() {
            LocalDate date = LocalDate.of(2025, 11, 10);
            Pageable pageable = PageRequest.of(0, 10);

            Page<DailySettlementResponse> page =
                    emptyDailyResponse.createEmptyDaily(date, pageable);

            assertThat(page.getTotalElements()).isZero();  // 실패해야 정상
        }
    }

}