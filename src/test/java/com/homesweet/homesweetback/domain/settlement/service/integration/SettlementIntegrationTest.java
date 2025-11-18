package com.homesweet.homesweetback.domain.settlement.service.integration;

import com.homesweet.homesweetback.common.exception.BusinessException;
import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.grade.entity.Grade;
import com.homesweet.homesweetback.domain.grade.repository.GradeRepository;
import com.homesweet.homesweetback.domain.order.entity.DeliveryStatus;
import com.homesweet.homesweetback.domain.order.entity.Order;
import com.homesweet.homesweetback.domain.order.entity.OrderStatus;
import com.homesweet.homesweetback.domain.product.product.repository.jpa.entity.SkuEntity;
import com.homesweet.homesweetback.domain.settlement.data.HelpIntegrationData;
import com.homesweet.homesweetback.domain.settlement.entity.Settlement;
import com.homesweet.homesweetback.domain.settlement.repository.SettlementRepository;
import com.homesweet.homesweetback.domain.settlement.service.SettlementService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("SettlementService 통합 테스트")
public class SettlementIntegrationTest {
    @Autowired
    private SettlementService settlementService;
    @Autowired
    private HelpIntegrationData helpIntegrationData;
    @Autowired
    private SettlementRepository settlementRepository;

    @Autowired
    private GradeRepository gradeRepository;
    @PersistenceContext
    EntityManager em;


    @Nested
    @DisplayName("성공 케이스")
    class Success {
        @Test
        @DisplayName("정산 생성")
        void createSettlement() {
            Grade grade = gradeRepository.findById(5).orElseThrow();
            User seller = helpIntegrationData.createSeller(grade);
            User buyer = helpIntegrationData.createBuyer();
            SkuEntity sku = helpIntegrationData.createSku(seller, "냄비", 35000);
            Order order = helpIntegrationData.createOrder(buyer, sku, 35000, LocalDateTime.of(2025, 11, 17, 9, 24));
            settlementService.createSettlement(order);

            // 판매금액
            Settlement saved = settlementRepository.findByOrderId(order.getId()).orElseThrow(() -> new IllegalArgumentException("정산이 생성되지 않음"));
            assertThat(saved.getSalesAmount()).isEqualByComparingTo(BigDecimal.valueOf(35000));
            // 수수료
            BigDecimal feeRate = BigDecimal.valueOf(35000).multiply(BigDecimal.valueOf(0.25));
            assertThat(saved.getFee()).isEqualByComparingTo(feeRate);
            // 부가세
            BigDecimal vat = BigDecimal.valueOf(35000).multiply(BigDecimal.valueOf(0.10));
            assertThat(saved.getVat()).isEqualByComparingTo(vat);
            // 정산금액
            BigDecimal settlementAmount = saved.getSalesAmount().subtract(saved.getFee()).subtract(BigDecimal.ZERO);
            assertThat(saved.getSettlementAmount()).isEqualByComparingTo(settlementAmount);
            // 판매자
            assertThat(saved.getUserId()).isEqualTo(seller.getId());
            System.out.println("saved: : " + saved.getSettlementId());
            em.flush(); // SQL 즉시 실행 → 로그 바로 확인 가능
        }
    }

    @Nested
    @DisplayName("실패 테스트")
    class Failure {
        @Test
        @DisplayName("배송 완료가 아니면 정산 불가")
        void createSettlement_fail_deliveryStatus() {

            Grade grade = gradeRepository.findById(5).orElseThrow();
            User seller = helpIntegrationData.createSeller(grade);
            User buyer = helpIntegrationData.createBuyer();
            SkuEntity sku = helpIntegrationData.createSku(seller, "상품", 10000);

            Order order = helpIntegrationData.createOrder(
                    buyer, sku, 10000,
                    LocalDateTime.now()
            );
            order.setDeliveryStatus(DeliveryStatus.DELIVERING);

            assertThatThrownBy(() -> settlementService.createSettlement(order))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("주문 상태가 결제 완료가 아니면 정산 불가")
        void createSettlement_fail_order_not_completed() {

            Grade grade = gradeRepository.findById(5).orElseThrow();
            User seller = helpIntegrationData.createSeller(grade);
            User buyer = helpIntegrationData.createBuyer();
            SkuEntity sku = helpIntegrationData.createSku(seller, "상품", 10000);

            Order order = helpIntegrationData.createOrder(
                    buyer,
                    sku,
                    10000,
                    LocalDateTime.now()
            );

            order.setOrderStatus(OrderStatus.PENDING);

            assertThatThrownBy(() -> settlementService.createSettlement(order))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ErrorCode.INVALID_ORDER_STATUS.getMessage());
        }

        @Test
        @DisplayName("이미 정산된 주문이면 중복 정산 예외")
        void createSettlement_fail_duplicate() {

            Grade grade = gradeRepository.findById(5).orElseThrow();
            User seller = helpIntegrationData.createSeller(grade);
            User buyer = helpIntegrationData.createBuyer();
            SkuEntity sku = helpIntegrationData.createSku(seller, "상품", 10000);

            Order order = helpIntegrationData.createOrder(
                    buyer, sku, 10000, LocalDateTime.now()
            );

            // 첫 번째 정산
            settlementService.createSettlement(order);

            // 두 번째 시도 → 중복
            assertThatThrownBy(() -> settlementService.createSettlement(order))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ErrorCode.DUPLICATE_SETTLEMENT.getMessage());
        }
    }

}
