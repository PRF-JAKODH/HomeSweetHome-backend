package com.homesweet.homesweetback.domain.settlement.service;

import com.homesweet.homesweetback.common.exception.BusinessException;
import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.grade.entity.Grade;
import com.homesweet.homesweetback.domain.grade.service.GradeService;
import com.homesweet.homesweetback.domain.order.entity.DeliveryStatus;
import com.homesweet.homesweetback.domain.order.entity.Order;
import com.homesweet.homesweetback.domain.order.entity.OrderItem;
import com.homesweet.homesweetback.domain.order.entity.OrderStatus;
import com.homesweet.homesweetback.domain.product.category.repository.jpa.entity.ProductCategoryEntity;
import com.homesweet.homesweetback.domain.product.product.repository.jpa.entity.ProductEntity;
import com.homesweet.homesweetback.domain.product.product.repository.jpa.entity.SkuEntity;
import com.homesweet.homesweetback.domain.settlement.data.HelperData;
import com.homesweet.homesweetback.domain.settlement.repository.SettlementRepository;
import com.homesweet.homesweetback.domain.settlement.util.ExtractedSeller;
import com.homesweet.homesweetback.domain.settlement.util.SettlementCalculator;
import com.homesweet.homesweetback.domain.settlement.validation.SettlementValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Nested;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
@DisplayName("정산 생성 테스트")
class SettlementServiceTest {

    @InjectMocks
    private SettlementService settlementService;

    @Mock
    private SettlementRepository settlementRepository;
    @Mock
    private GradeService gradeService;

    @Mock
    private SettlementValidator settlementValidator;

    @Mock
    private ExtractedSeller extractedSeller;

    @Mock
    private SettlementCalculator settlementCalculator;


    @Nested
    @DisplayName("정산 생성 성공 케이스")
    class CreateSettlement {
        // 성공
        @Test
        @DisplayName("주문이 결제 완료 & 배송 완료일 때 정산을 생성합니다.")
        void createSettlement_Success() {
            // given
            Grade grade = HelperData.getGrade();
            User seller = HelperData.getSeller(grade);
            User user = HelperData.getUser();
            ProductCategoryEntity category = HelperData.getCategory();
            ProductEntity product = HelperData.getProduct(seller, category);
            SkuEntity sku = HelperData.getSkuEntity(product);
            Order order = HelperData.getOrder(user);
            OrderItem orderItem = HelperData.getOrderItem(sku);
            order.addOrderItem(orderItem);
            // 강제 주입 --> order.getId()
            ReflectionTestUtils.setField(order, "id", 1L);
            ReflectionTestUtils.setField(order, "totalAmount", 150000L);
            // 호출되는 mock
            given(settlementCalculator.getResult(any(Order.class), any(User.class)))
                    .willReturn(new SettlementCalculator.Result(
                            BigDecimal.valueOf(7500),        // fee
                            BigDecimal.ZERO,                 // refundAmount
                            BigDecimal.valueOf(15000),       // vat
                            BigDecimal.valueOf(150000),      // totalAmount
                            BigDecimal.valueOf(142500)       // settlementAmount
                    ));
            given(extractedSeller.extractSeller(any(Order.class))).willReturn(seller);
            // when
            settlementService.createSettlement(order);

            // then
            then(settlementRepository).should(times(1)).save(any());
            then(settlementCalculator).should(times(1)).getResult(order, seller);
        }
    }

    // 실패
    // 주문 상태가 주문 완료가 아닙니다.
    // 배송 상태가 배송완료가 아닙니다.
    // 존재하지 않은 주문 제품입니다.
    // User의 role이 SELLER가 아닙니다.
    // 판매자가 존재하지 않습니다.
    // 기존 주문 건이 있습니다.
    // 취소된 주문 건입니다.
    // - 계산
    // totalAmount == null
    // < 0(음수)
    @Nested
    @DisplayName("실패 케이스")
    class Fail {
        @Test
        @DisplayName("주문 상태가 주문 완료가 아니면 예외 발생")
        void orderStatusFailure_NotCompleted() {
            // given
            Order order = HelperData.getOrder(HelperData.getUser());
            Grade grade = HelperData.getGrade();
            User seller = HelperData.getSeller(grade);
            order.setOrderStatus(OrderStatus.PENDING);
            // validator의 동작 지정
            doThrow(new BusinessException(ErrorCode.SETTLEMENT_NOT_CREATED))
                    .when(settlementValidator)
                    .validateOrder(any(Order.class));
            // when
            // then
            assertThatThrownBy(() -> settlementService.createSettlement(order))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ErrorCode.SETTLEMENT_NOT_CREATED.getMessage());
        }

        @Test
        @DisplayName("배송 상태가 배송중이면 예외 발생")
        void deliveryStatusFailure_Delivering() {
            // given
            Order order = HelperData.getOrder(HelperData.getUser());
            order.setDeliveryStatus(DeliveryStatus.DELIVERING);
            doThrow(new BusinessException(ErrorCode.DELIVERY_STATUS_NOT_DELIVERED))
                    .when(settlementValidator)
                    .validateOrder(any(Order.class));
            // when
            // then
            assertThatThrownBy(() -> settlementService.createSettlement(order))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ErrorCode.DELIVERY_STATUS_NOT_DELIVERED.getMessage());
            then(settlementValidator).should(times(1)).validateOrder(order);
        }

//        @Test
//        @DisplayName("판매자 정보가 없으면 예외 발생")
//        void validateSeller_NotFound_Failure() {
//            assertThatThrownBy(() -> settlementValidator.validateSeller(null))
//                    .isInstanceOf(BusinessException.class)
//                    .hasMessage(ErrorCode.SELLER_NOT_FOUND.getMessage());
//        }

        @Test
        @DisplayName("User의 Role이 SELLER가 아니면 예외 발생")
        void roleCheckFailure() {
            Order order = HelperData.getOrder(HelperData.getUser());
            User seller = HelperData.getSeller(HelperData.getGrade());
            User user = HelperData.getUser();
            given(extractedSeller.extractSeller(any(Order.class))).willReturn(HelperData.getUser());

            doThrow(new BusinessException(ErrorCode.INVALID_SELLER_ROLE))
                    .when(settlementValidator)
                    .validateSeller(any(User.class));

            assertThatThrownBy(() -> settlementService.createSettlement(order))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ErrorCode.INVALID_SELLER_ROLE.getMessage());
            then(settlementValidator).should(times(1)).validateSeller(any(User.class));
        }

        // 기존 주문 건인지(중복 방지)
        @Test
        @DisplayName("정산 완료된 주문 건이면 예외 발생")
        void SettlementCompleted() {
            // given
            Order order = HelperData.getOrder(HelperData.getUser());
            User seller = HelperData.getSeller(HelperData.getGrade());

            doThrow(new BusinessException(ErrorCode.DUPLICATE_SETTLEMENT))
                    .when(settlementValidator)
                    .validateOrder(any(Order.class));

            // when & then
            assertThatThrownBy(() -> settlementService.createSettlement(order))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ErrorCode.DUPLICATE_SETTLEMENT.getMessage());

            then(settlementValidator).should(times(1)).validateOrder(order);
        }


        @Test
        @DisplayName("취소된 주문이면 정산 생성 예외 발생")
        void DeliveryStatusFailure() {
            Order order = HelperData.getOrder(HelperData.getUser());
            doThrow(new BusinessException(ErrorCode.ORDER_CANCELED_NOT_FOUND))
                    .when(settlementValidator)
                    .validateOrder(any(Order.class));

            // when & then
            assertThatThrownBy(() -> settlementService.createSettlement(order))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ErrorCode.ORDER_CANCELED_NOT_FOUND.getMessage());

            then(settlementValidator).should(times(1)).validateOrder(order);
        }
    }
}