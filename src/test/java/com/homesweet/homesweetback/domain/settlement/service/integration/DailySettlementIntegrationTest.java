package com.homesweet.homesweetback.domain.settlement.service.integration;

import com.homesweet.homesweetback.common.exception.BusinessException;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.grade.entity.Grade;
import com.homesweet.homesweetback.domain.grade.repository.GradeRepository;
import com.homesweet.homesweetback.domain.order.entity.Order;
import com.homesweet.homesweetback.domain.product.product.command.repository.jpa.entity.SkuEntity;
import com.homesweet.homesweetback.domain.settlement.data.HelpIntegrationData;
import com.homesweet.homesweetback.domain.settlement.dto.response.DailySettlementResponse;
import com.homesweet.homesweetback.domain.settlement.entity.DailySettlement;
import com.homesweet.homesweetback.domain.settlement.repository.SettlementRepository;
import com.homesweet.homesweetback.domain.settlement.service.DailySettlementService;
import com.homesweet.homesweetback.domain.settlement.service.SettlementService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("DailySettlementService 통합 테스트")
public class DailySettlementIntegrationTest {
    @Autowired
    private DailySettlementService dailySettlementService;
    @Autowired
    private HelpIntegrationData helpIntegrationData;

    @Autowired
    private GradeRepository gradeRepository;

    @Autowired
    private SettlementService settlementService;

    @Autowired
    private SettlementRepository settlementRepository;

    @Nested
    @DisplayName("성공 케이스")
    class Success {
        @Test
        @DisplayName("일별 집계 생성 성공")
        void dailySettlement_success() {

            Grade grade = gradeRepository.findById(5).orElseThrow();
            User seller = helpIntegrationData.createSeller(grade);
            User buyer = helpIntegrationData.createBuyer();
            SkuEntity sku = helpIntegrationData.createSku(seller, "주방도구", 35000);

            // 정산건 2개 생성
            Order o1 = helpIntegrationData.createOrder(buyer, sku, 35000,
                    LocalDateTime.of(2025, 11, 10, 10, 0));
            settlementService.createSettlement(o1);

            Order o2 = helpIntegrationData.createOrder(buyer, sku, 20000,
                    LocalDateTime.of(2025, 11, 10, 12, 0));
            settlementService.createSettlement(o2);

            dailySettlementService.getSettlement(
                    seller.getId(),
                    LocalDateTime.of(2025, 11, 1, 0, 0),
                    LocalDateTime.of(2025, 11, 30, 23, 59)
            );

            // when
            Pageable pageable = PageRequest.of(0, 10);

            Page<DailySettlementResponse> daily = dailySettlementService.getDailySummary(
                    seller.getId(),
                    LocalDate.of(2025, 11, 1),
                    LocalDate.of(2025, 11, 30),
                    pageable
            );
            // then
            DailySettlementResponse res = daily.getContent().get(0);

            assertThat(res.totalSales())
                    .isEqualByComparingTo("55000");

            assertThat(res.totalFee())
                    .isEqualByComparingTo("13750");

            assertThat(res.totalSettlement())
                    .isEqualByComparingTo("41250");
        }

        @Test
        @DisplayName("일별 집계 - 데이터 없으면 빈 페이지 반환")
        void dailySettlement_empty() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<DailySettlementResponse> daily = dailySettlementService.getDailySummary(
                    999L, // 존재하지 않는 sellerId
                    LocalDate.of(2025, 11, 10),
                    LocalDate.of(2025, 11, 10),
                    pageable
            );

            assertThat(daily.getTotalElements()).isEqualTo(1);
            DailySettlementResponse res = daily.getContent().get(0);
            assertThat(res.totalSales()).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("일별 집계 - now() 기준")
        void dailySettlement_nowBased() {
            Grade grade = gradeRepository.findById(5).orElseThrow();
            User seller = helpIntegrationData.createSeller(grade);
            User buyer = helpIntegrationData.createBuyer();
            SkuEntity sku = helpIntegrationData.createSku(seller, "상품", 10000);

            // settlementDate = now() 로 저장됨
            Order o1 = helpIntegrationData.createOrder(buyer, sku, 10000, LocalDateTime.now());
            settlementService.createSettlement(o1);

            Order o2 = helpIntegrationData.createOrder(buyer, sku, 20000, LocalDateTime.now());
            settlementService.createSettlement(o2);

            // ★ now 기준 집계 실행
            LocalDateTime now = LocalDateTime.now();
            dailySettlementService.getSettlement(
                    seller.getId(),
                    now.minusMinutes(1),
                    now.plusMinutes(1)
            );

            Pageable pageable = PageRequest.of(0, 10);

            // now 기준
            LocalDate today = LocalDate.now();
            Page<DailySettlementResponse> daily = dailySettlementService.getDailySummary(
                    seller.getId(),
                    today,
                    today,
                    pageable
            );
            DailySettlementResponse res = daily.getContent().get(0);
            assertThat(res.totalSales()).isEqualByComparingTo("30000");
        }
        @Test
        @DisplayName("일별 집계 - 페이징 동작 확인")
        void dailySettlement_paging() {

            Grade grade = gradeRepository.findById(5).orElseThrow();
            User seller = helpIntegrationData.createSeller(grade);
            User buyer = helpIntegrationData.createBuyer();
            SkuEntity sku = helpIntegrationData.createSku(seller, "상품", 10000);

            // ✔ 주문일자는 의미 없음 (정산일 now() 기준)
            settlementService.createSettlement(
                    helpIntegrationData.createOrder(buyer, sku, 10000, LocalDateTime.now())
            );

            settlementService.createSettlement(
                    helpIntegrationData.createOrder(buyer, sku, 20000, LocalDateTime.now())
            );

            settlementService.createSettlement(
                    helpIntegrationData.createOrder(buyer, sku, 30000, LocalDateTime.now())
            );
            // ★★★ now 기준 집계 실행 ★★★
            LocalDateTime now = LocalDateTime.now();

            dailySettlementService.getSettlement(
                    seller.getId(),
                    now.minusMinutes(1),
                    now.plusMinutes(1)
            );
            // now() 날짜 기준 조회
            LocalDate today = LocalDate.now();

            Page<DailySettlementResponse> page = dailySettlementService.getDailySummary(
                    seller.getId(),
                    today,
                    today,
                    PageRequest.of(0, 10)
            );
            // then
            assertThat(page.getTotalElements()).isEqualTo(1); // ★ 날짜당 1개 집계이므로 1개
        }

        @Test
        @DisplayName("일별 집계 - getSettlement() 호출 시 DailySettlement 테이블에 저장됨")
        void dailySettlement_saveByGetSettlement() {

            Grade grade = gradeRepository.findById(5).orElseThrow();
            User seller = helpIntegrationData.createSeller(grade);
            User buyer = helpIntegrationData.createBuyer();
            SkuEntity sku = helpIntegrationData.createSku(seller, "상품", 20000);

            LocalDateTime fixedNow = LocalDateTime.of(2025, 11, 10, 10, 0);

            // 주문 생성
            Order order = helpIntegrationData.createOrder(buyer, sku, 20000, fixedNow);

            // 정산 생성 (고정 now 사용)
            settlementService.createSettlement(order);
            // test용으로 settlementDate를 강제로 fixedNow로 변경
            settlementRepository.findByOrderId(order.getId())
                    .ifPresent(s -> {
                        s.setSettlementDate(fixedNow);
                        settlementRepository.save(s);
                    });

            // 집계 범위
            LocalDateTime start = fixedNow.toLocalDate().atStartOfDay();
            LocalDateTime end = start.plusDays(1);

            dailySettlementService.getSettlement(seller.getId(), start, end);

            Page<DailySettlement> all = dailySettlementService.findDailySettlements(
                    seller.getId(),
                    PageRequest.of(0, 10),
                    start,
                    end
            );

            assertThat(all.getTotalElements()).isEqualTo(1);

            DailySettlement saved = all.getContent().get(0);
            assertThat(saved.getTotalSales()).isEqualByComparingTo("20000");
        }
    }
    @Nested
    @DisplayName("실패 케이스")
    class Failure{
        @Test
        @DisplayName("일별 집계 실패 - 집계할 정산 데이터가 없음")
        void dailySettlement_fail_noSettlements() {
            Grade grade = gradeRepository.findById(5).orElseThrow();
            User seller = helpIntegrationData.createSeller(grade);

            // settlement를 아예 생성하지 않는다.

            LocalDateTime start = LocalDateTime.of(2025, 11, 10, 0, 0);
            LocalDateTime end   = LocalDateTime.of(2025, 11, 11, 0, 0);

            assertThatThrownBy(() ->
                    dailySettlementService.getSettlement(
                            seller.getId(),
                            start,
                            end
                    )
            ).isInstanceOf(BusinessException.class)
                    .hasMessageContaining("조회된 정산 데이터가 없습니다.");
        }
        @Test
        @DisplayName("일별 집계 실패 - 날짜 범위에 해당하는 정산이 없음")
        void dailySettlement_fail_wrongDateRange() {

            Grade grade = gradeRepository.findById(5).orElseThrow();
            User seller = helpIntegrationData.createSeller(grade);
            User buyer = helpIntegrationData.createBuyer();
            SkuEntity sku = helpIntegrationData.createSku(seller, "상품", 30000);

            // settlementDate = 2025-11-15
            LocalDateTime settledAt = LocalDateTime.of(2025, 11, 15, 10, 0);

            Order o = helpIntegrationData.createOrder(buyer, sku, 30000, settledAt);
            settlementService.createSettlement(o);

            // settlementDate 강제 고정
            settlementRepository.findByOrderId(o.getId())
                    .ifPresent(s -> {
                        s.setSettlementDate(settledAt);
                        settlementRepository.save(s);
                    });

            // → 집계할 범위는 11월 10일 하루. 정산은 15일.
            LocalDateTime start = LocalDateTime.of(2025, 11, 10, 0, 0);
            LocalDateTime end   = LocalDateTime.of(2025, 11, 11, 0, 0);

            assertThatThrownBy(() ->
                    dailySettlementService.getSettlement(
                            seller.getId(),
                            start,
                            end
                    )
            ).isInstanceOf(BusinessException.class)
                    .hasMessageContaining("조회된 정산 데이터가 없습니다.");
        }
        @Test
        @DisplayName("일별 집계 실패 - 잘못된 날짜 범위(start >= end)")
        void dailySettlement_fail_invalidRange() {

            Grade grade = gradeRepository.findById(5).orElseThrow();
            User seller = helpIntegrationData.createSeller(grade);

            LocalDateTime t = LocalDateTime.of(2025, 11, 10, 0, 0);

            LocalDateTime start = t;
            LocalDateTime end   = t;  // 잘못된 범위

            assertThatThrownBy(() ->
                    dailySettlementService.getSettlement(
                            seller.getId(),
                            start,
                            end
                    )
            ).isInstanceOf(BusinessException.class);
        }
    }
}