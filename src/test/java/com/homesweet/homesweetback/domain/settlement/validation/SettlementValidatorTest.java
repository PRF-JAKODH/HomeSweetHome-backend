package com.homesweet.homesweetback.domain.settlement.validation;

import com.homesweet.homesweetback.common.exception.BusinessException;
import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.grade.entity.Grade;
import com.homesweet.homesweetback.domain.order.entity.DeliveryStatus;
import com.homesweet.homesweetback.domain.order.entity.Order;
import com.homesweet.homesweetback.domain.order.entity.OrderItem;
import com.homesweet.homesweetback.domain.order.entity.OrderStatus;
import com.homesweet.homesweetback.domain.product.category.repository.jpa.entity.ProductCategoryEntity;
import com.homesweet.homesweetback.domain.product.product.repository.jpa.entity.ProductEntity;
import com.homesweet.homesweetback.domain.product.product.repository.jpa.entity.SkuEntity;
import com.homesweet.homesweetback.domain.settlement.data.HelperData;
import com.homesweet.homesweetback.domain.settlement.entity.DailySettlement;
import com.homesweet.homesweetback.domain.settlement.entity.MonthlySettlement;
import com.homesweet.homesweetback.domain.settlement.entity.Settlement;
import com.homesweet.homesweetback.domain.settlement.entity.WeeklySettlement;
import com.homesweet.homesweetback.domain.settlement.repository.SettlementRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SettlementValidator 검증 테스트")
public class SettlementValidatorTest {

    @InjectMocks
    private SettlementValidator settlementValidator;

    @Mock
    private SettlementRepository settlementRepository;

    @Nested
    @DisplayName("성공 케이스")
    class Success {
        // 주문 검증
        @Test
        @DisplayName("정산을 하기 위한 주문에 대한 검증")
        void validateOrder_Success() {
            // given
            Order order = HelperData.getOrder(HelperData.getUser());
            order.setOrderStatus(OrderStatus.COMPLETED);
            order.setDeliveryStatus(DeliveryStatus.DELIVERED);

            Grade grade = HelperData.getGrade();
            User seller = HelperData.getSeller(grade);
            User user = HelperData.getUser();
            ProductCategoryEntity category = HelperData.getCategory();
            ProductEntity product = HelperData.getProduct(seller, category);
            SkuEntity sku = HelperData.getSkuEntity(product);
            OrderItem orderItem = HelperData.getOrderItem(sku);
            order.addOrderItem(orderItem);

            assertThatCode(() -> settlementValidator.validateOrder(order)).doesNotThrowAnyException();
        }

        // 판매자 검증
        @Test
        @DisplayName("판매자에 대한 검증")
        void validateSeller_Success() {
            // given
            User seller = HelperData.getSeller(HelperData.getGrade());

            assertThatCode(() -> settlementValidator.validateSeller(seller)).doesNotThrowAnyException();
        }

        // 주문 취소시 환불 금액 반영
        @Test
        @DisplayName("환불 및 정산 금액 반영에 대한 검증-정산상태: COMPLETED")
        void validateCanceled_StatusIsCompleted_Success() {
            Settlement settlement = HelperData.getSettlement();
            settlement.setSettlementStatus("COMPLETED");
            Order order = HelperData.getOrder(HelperData.getUser());
            order.setDeliveryStatus(DeliveryStatus.CANCELLED);

            assertThatCode(() -> settlementValidator.validateCanceled(settlement, order)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("환불 및 정산 금액 반영에 대한 검증-정산상태: COMPLETED")
        void validateCanceled_StatusIsPending_Success() {
            Settlement settlement = HelperData.getSettlement();
            settlement.setSettlementStatus("PENDING");
            Order order = HelperData.getOrder(HelperData.getUser());
            order.setDeliveryStatus(DeliveryStatus.CANCELLED);

            assertThatCode(() -> settlementValidator.validateCanceled(settlement, order)).doesNotThrowAnyException();
        }

        // 정산 내역이 있는지
        @Test
        @DisplayName("집계를 하기 위해 주문건별 정산내역이 있는지에 대한 검증")
        void validateDaily_Success() {
            Settlement settlement = HelperData.getSettlement();
            List<Settlement> settlements = List.of(settlement);

            assertThatCode(() -> settlementValidator.validateDaily(settlements)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("WeeklyValidate - 리스트가 비어있지 않으면 성공")
        void validateWeekly_success() {
            List<DailySettlement> settlements = List.of(
                    HelperData.getDailySettlementWithDate(LocalDate.now())
            );

            assertThatCode(() -> settlementValidator.validateWeekly(settlements))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("MonthlyValidate - 리스트가 비어있지 않으면 성공")
        void validateMonthly_success() {
            WeeklySettlement ws = HelperData.getWeeklySettlementWithDate(LocalDate.now());

            List<WeeklySettlement> settlements = List.of(ws);

            assertThatCode(() -> settlementValidator.validateMonthly(settlements))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("연별 정산 리스트가 비어있지 않으면 성공")
        void validateYearly_success() {
            // given
            MonthlySettlement ms = new MonthlySettlement();
            List<MonthlySettlement> list = List.of(ms);

            settlementValidator.validateYearly(list);
        }
    }
    @Nested
    @DisplayName("실패 케이스")
    class Fail {
        @Nested
        @DisplayName("정산 생성 예외 발생 케이스")
        class ValidateOrder {
            @Test
            @DisplayName("주문이 존재하지 않으면 예외 발생")
            void validateOrder_NotExistOrder_Failure() {
                assertThatThrownBy(() -> settlementValidator.validateOrder(null))
                        .isInstanceOf(BusinessException.class)
                        .hasMessage(ErrorCode.ORDERS_NOT_FOUND.getMessage());
            }
            @Test
            @DisplayName("주문 상태가 주문 완료가 아니면 예외 발생")
            void orderStatusFailure_NotCompleted() {
                // given
                Order order = HelperData.getOrder(HelperData.getUser());
                order.setDeliveryStatus(DeliveryStatus.DELIVERED);
                order.setOrderStatus(OrderStatus.PENDING);

                assertThatThrownBy(() -> settlementValidator.validateOrder(order))
                        .isInstanceOf(BusinessException.class)
                        .hasMessage(ErrorCode.INVALID_ORDER_STATUS.getMessage());
            }
            @Test
            @DisplayName("배송상태가 주문 취소된 건이면 예외 발생")
            void deliveryCanceled_Order_Failure() {
                Order order = HelperData.getOrder(HelperData.getUser());
                User seller = HelperData.getSeller(HelperData.getGrade());
                ProductCategoryEntity productCategory = HelperData.getCategory();
                ProductEntity product = HelperData.getProduct(seller, productCategory);
                SkuEntity sku = HelperData.getSkuEntity(product);
                OrderItem orderItem = HelperData.getOrderItem(sku);

                order.setOrderStatus(OrderStatus.COMPLETED);
                order.setDeliveryStatus(DeliveryStatus.CANCELLED);
                order.addOrderItem(orderItem);

                assertThatThrownBy(() -> settlementValidator.validateOrder(order))
                        .isInstanceOf(BusinessException.class)
                        .hasMessage(ErrorCode.ORDER_CANCELED_NOT_FOUND.getMessage());
            }
            @Test
            @DisplayName("배송 상태가 배송중이면 예외 발생")
            void deliveryStatusFailure_Delivering() {
                // given
                Order order = HelperData.getOrder(HelperData.getUser());
                order.setOrderStatus(OrderStatus.COMPLETED);
                order.setDeliveryStatus(DeliveryStatus.DELIVERING);

                assertThatThrownBy(() -> settlementValidator.validateOrder(order))
                        .isInstanceOf(BusinessException.class)
                        .hasMessage(ErrorCode.DELIVERY_STATUS_NOT_DELIVERED.getMessage());
            }
            @Test
            @DisplayName("배송 상태가 배송전이면 예외 발생")
            void deliveryStatusFailure_Before_shipment() {
                // given
                Order order = HelperData.getOrder(HelperData.getUser());
                order.setOrderStatus(OrderStatus.COMPLETED);
                order.setDeliveryStatus(DeliveryStatus.BEFORE_SHIPMENT);

                assertThatThrownBy(() -> settlementValidator.validateOrder(order))
                        .isInstanceOf(BusinessException.class)
                        .hasMessage(ErrorCode.DELIVERY_STATUS_NOT_DELIVERED.getMessage());
            }
            @Test
            @DisplayName("주문 제품이 비어있으면 예외 발생")
            void notFound_OrderItems_Failure() {
                // given
                Order order = HelperData.getOrder(HelperData.getUser());
                order.setOrderStatus(OrderStatus.COMPLETED);
                order.setDeliveryStatus(DeliveryStatus.DELIVERED);
                order.getOrderItems().clear();

                assertThatThrownBy(() -> settlementValidator.validateOrder(order))
                        .isInstanceOf(BusinessException.class)
                        .hasMessage(ErrorCode.ORDER_ITEMS_EMPTY.getMessage());
            }
            @Test
            @DisplayName("중복된 주문건이면 예외 발생")
            void duplicated_Order_Failure() {
                Order order = HelperData.getOrder(HelperData.getUser());
                User seller = HelperData.getSeller(HelperData.getGrade());
                ProductCategoryEntity productCategory = HelperData.getCategory();
                ProductEntity product = HelperData.getProduct(seller, productCategory);
                SkuEntity sku = HelperData.getSkuEntity(product);
                OrderItem orderItem = HelperData.getOrderItem(sku);
                order.setOrderStatus(OrderStatus.COMPLETED);
                order.setDeliveryStatus(DeliveryStatus.DELIVERED);
                order.addOrderItem(orderItem);
                ReflectionTestUtils.setField(order, "id", 1L);

                given(settlementRepository.findByOrderId(1L))
                        .willReturn(Optional.of(new Settlement()));

                assertThatThrownBy(() -> settlementValidator.validateOrder(order))
                        .isInstanceOf(BusinessException.class)
                        .hasMessage(ErrorCode.DUPLICATE_SETTLEMENT.getMessage());
            }
        }
        @Nested
        @DisplayName("판매자 추출시 예외 케이스")
        class extractedSeller_Fail {
            @Test
            @DisplayName("판매자 정보가 없으면 예외 발생")
            void validateSeller_NotFound_Failure() {
                assertThatThrownBy(() -> settlementValidator.validateSeller(null))
                        .isInstanceOf(BusinessException.class)
                        .hasMessage(ErrorCode.SELLER_NOT_FOUND.getMessage());
            }
            @Test
            @DisplayName("User의 Role이 SELLER가 아니면 예외 발생")
            void roleCheckFailure() {
                Order order = HelperData.getOrder(HelperData.getUser());
                User seller = HelperData.getSeller(HelperData.getGrade());
                User user = HelperData.getUser();

                assertThatThrownBy(() -> settlementValidator.validateSeller(user))
                        .isInstanceOf(BusinessException.class)
                        .hasMessage(ErrorCode.SELLER_NOT_FOUND.getMessage());
            }
        }
        @Nested
        @DisplayName("환불시 정산 취소 발생 예외 케이스")
        class refundSettlement_Fail {
            @Test
            @DisplayName("이미 취소된 정산이면 예외 발생")
            void settlementAlready_Failure() {
                Order order = HelperData.getOrder(HelperData.getUser());
                order.setDeliveryStatus(DeliveryStatus.CANCELLED);
                Settlement settlement = HelperData.getSettlement();
                settlement.setSettlementStatus("CANCELED");

                assertThatThrownBy(() -> settlementValidator.validateCanceled(settlement, order))
                        .isInstanceOf(BusinessException.class)
                        .hasMessage(ErrorCode.ALREADY_SETTLEMENT_CANCELED.getMessage());
            }
            @Test
            @DisplayName("배송상태가 배송완료라면 환불받을 수 없는 예외 발생")
            void deliveryStatus_delivered_not_caneceled_Failure() {
                Order order = HelperData.getOrder(HelperData.getUser());
                Settlement settlement = HelperData.getSettlement();
                order.setDeliveryStatus(DeliveryStatus.DELIVERED);

                assertThatThrownBy(() -> settlementValidator.validateCanceled(settlement, order))
                        .isInstanceOf(BusinessException.class)
                        .hasMessage(ErrorCode.INVALID_ORDER_STATUS.getMessage());

            }
            @Test
            @DisplayName("배송상태가 배송중이라면 환불받을 수 없는 예외 발생")
            void deliveryStatus_delivering_not_caneceled_Failure() {
                Order order = HelperData.getOrder(HelperData.getUser());
                Settlement settlement = HelperData.getSettlement();
                order.setDeliveryStatus(DeliveryStatus.DELIVERING);

                assertThatThrownBy(() -> settlementValidator.validateCanceled(settlement, order))
                        .isInstanceOf(BusinessException.class)
                        .hasMessage(ErrorCode.INVALID_ORDER_STATUS.getMessage());
            }
            @Test
            @DisplayName("배송상태가 배송전이라면 환불받을 수 없는 예외 발생")
            void deliveryStatus_before_shipping_not_caneceled_Failure() {
                Order order = HelperData.getOrder(HelperData.getUser());
                Settlement settlement = HelperData.getSettlement();
                order.setDeliveryStatus(DeliveryStatus.BEFORE_SHIPMENT);

                assertThatThrownBy(() -> settlementValidator.validateCanceled(settlement, order))
                        .isInstanceOf(BusinessException.class)
                        .hasMessage(ErrorCode.INVALID_ORDER_STATUS.getMessage());
            }
            @Nested
            @DisplayName("집계시 기간별 집계정산 예외 케이스")
            class aggregate_Fail {
                @Test
                @DisplayName("정산 데이터가 null이면 예외 발생")
                void validateDaily_null_Failure() {
                    assertThatThrownBy(() -> settlementValidator.validateDaily(null))
                            .isInstanceOf(BusinessException.class)
                            .hasMessage(ErrorCode.SETTLEMENT_NOT_FOUND.getMessage());
                }
                @Test
                @DisplayName("정산 데이터가 빈 리스트면 예외 발생")
                void validateDaily_empty_list_Failure() {
                    assertThatThrownBy(() -> settlementValidator.validateDaily(Collections.emptyList()))
                            .isInstanceOf(BusinessException.class)
                            .hasMessage(ErrorCode.SETTLEMENT_NOT_FOUND.getMessage());
                }
                @Test
                @DisplayName("WeeklyValidate - 리스트가 null이면 BusinessException 발생")
                void validateWeekly_fail_nullList() {

                    assertThatThrownBy(() -> settlementValidator.validateWeekly(null))
                            .isInstanceOf(BusinessException.class)
                            .hasMessage(ErrorCode.SETTLEMENT_NOT_FOUND.getMessage());
                }
                @Test
                @DisplayName("WeeklyValidate - 리스트가 비어있으면 BusinessException")
                void validateWeekly_fail_emptyList() {

                    assertThatThrownBy(() -> settlementValidator.validateWeekly(List.of()))
                            .isInstanceOf(BusinessException.class)
                            .hasMessage(ErrorCode.SETTLEMENT_NOT_FOUND.getMessage());
                }
                @Test
                @DisplayName("[실패] MonthlyValidate - 리스트가 null이면 BusinessException")
                void validateMonthly_fail_nullList() {

                    assertThatThrownBy(() -> settlementValidator.validateMonthly(null))
                            .isInstanceOf(BusinessException.class)
                            .hasMessage(ErrorCode.SETTLEMENT_NOT_FOUND.getMessage());
                }
                @Test
                @DisplayName("MonthlyValidate - 리스트가 비어있으면 BusinessException")
                void validateMonthly_fail_emptyList() {

                    assertThatThrownBy(() -> settlementValidator.validateMonthly(List.of()))
                            .isInstanceOf(BusinessException.class)
                            .hasMessage(ErrorCode.SETTLEMENT_NOT_FOUND.getMessage());
                }
                @Test
                @DisplayName("연별 정산 리스트가 비어있으면 BusinessException 발생")
                void validateYearly_fail_emptyList() {
                    // given
                    List<MonthlySettlement> emptyList = List.of();

                    // when & then
                    assertThatThrownBy(() -> settlementValidator.validateYearly(emptyList))
                            .isInstanceOf(BusinessException.class)
                            .hasMessage(ErrorCode.SETTLEMENT_NOT_FOUND.getMessage());
                }
                @Test
                @DisplayName("연별 정산 리스트가 null이면 BusinessException 발생")
                void validateYearly_fail_null() {
                    // when & then
                    assertThatThrownBy(() -> settlementValidator.validateYearly(null))
                            .isInstanceOf(BusinessException.class)
                            .hasMessage(ErrorCode.SETTLEMENT_NOT_FOUND.getMessage());
                }
            }
        }
    }
}
