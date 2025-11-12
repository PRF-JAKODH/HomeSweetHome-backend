package com.homesweet.homesweetback.domain.settlement.service;

import com.homesweet.homesweetback.common.exception.BusinessException;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.entity.UserRole;
import com.homesweet.homesweetback.domain.grade.entity.Grade;
import com.homesweet.homesweetback.domain.grade.service.GradeService;
import com.homesweet.homesweetback.domain.order.entity.DeliveryStatus;
import com.homesweet.homesweetback.domain.order.entity.Order;
import com.homesweet.homesweetback.domain.order.entity.OrderItem;
import com.homesweet.homesweetback.domain.order.entity.OrderStatus;
import com.homesweet.homesweetback.domain.order.repository.OrderRepository;
import com.homesweet.homesweetback.domain.product.category.repository.jpa.entity.ProductCategoryEntity;
import com.homesweet.homesweetback.domain.product.product.repository.jpa.entity.ProductEntity;
import com.homesweet.homesweetback.domain.product.product.repository.jpa.entity.SkuEntity;
import com.homesweet.homesweetback.domain.settlement.repository.SettlementRepository;
import com.homesweet.homesweetback.domain.settlement.util.ExtractedSeller;
import com.homesweet.homesweetback.domain.settlement.util.SettlementCalculater;
import com.homesweet.homesweetback.domain.settlement.validation.SettlementValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Nested;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.assertj.core.api.Assertions.assertThat;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
@DisplayName("정산 서비스 단위 테스트")
class SettlementServiceTest {

    @InjectMocks
    private SettlementService settlementService;
    @Mock
    private SettlementRepository settlementRepository;
    @Mock
    private GradeService gradeService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private SettlementValidator settlementValidator;

    @InjectMocks
    private SettlementCalculater settlementCalculater;

    @InjectMocks
    private ExtractedSeller extractedSeller;

    @Nested
    @DisplayName("정산 생성")
    class CreateSettlement {

        @Nested
        @DisplayName("성공")
        class Success {

            // 성공
            @Test
            @DisplayName("주문이 결제 완료 & 배송 완료일 때 정산을 생성합니다.")
            void createSettlement_Success() {
                // given
                Grade grade = helper.getGrade();
                User seller = helper.getSeller(grade);
                User user = helper.getUser();
                ProductCategoryEntity category = helper.getCategory();
                ProductEntity product = helper.getProduct(seller, category);
                SkuEntity sku = helper.getSkuEntity(product);
                Order order = helper.getOrder(user);
                OrderItem orderItem = helper.getOrderItem(sku);

                order.addOrderItem(orderItem);
                // 강제 주입 --> order.getId()
                ReflectionTestUtils.setField(order, "id", 1L);
                // 호출되는 mock
                given(gradeService.calculateFeeforUser(any(), any())).willReturn(BigDecimal.valueOf(7500));
                // when
                settlementService.createSettlement(order);

                // then
                then(settlementRepository).should(Mockito.times(1)).save(any());
            }

            @Test
            @DisplayName("정산 금액을 계산합니다.")
            void calcSettlementAmount_Success() {
                // given
                Order order = helper.getOrder(helper.getUser());
                User seller = helper.getSeller(helper.getGrade());
                ReflectionTestUtils.setField(order, "totalAmount", 150000L);
                given(gradeService.calculateFeeforUser(BigDecimal.valueOf(150000L), seller)).willReturn(BigDecimal.valueOf(7500));
                // when
                SettlementCalculater.Result result = settlementCalculater.getResult(order, seller);

                // then
                assertThat(result).isNotNull();
                assertThat(result.vat()).isEqualTo(BigDecimal.valueOf(15000.00).setScale(2, RoundingMode.HALF_UP));
                assertThat(result.refundAmount()).isEqualTo(BigDecimal.ZERO);
                assertThat(result.settlementAmount()).isEqualTo(BigDecimal.valueOf(142500.00).setScale(2, RoundingMode.HALF_UP));
            }
        }

        // 판매자 확인
        @Test
        @DisplayName("판매자 정보를 추출한다.")
        void extractedSeller_Success() {
            // given
            Grade grade = helper.getGrade();
            User seller = helper.getSeller(grade);
            User user = helper.getUser();
            ProductCategoryEntity category = helper.getCategory();
            ProductEntity product = helper.getProduct(seller, category);
            SkuEntity sku = helper.getSkuEntity(product);
            Order order = helper.getOrder(user);
            OrderItem orderItem = helper.getOrderItem(sku);
            order.addOrderItem(orderItem);
            // when
            User extractedSeller = SettlementServiceTest.this.extractedSeller.extractSeller(order);
            // then
            assertThat(extractedSeller).isNotNull();
            assertThat(extractedSeller.getRole()).isEqualTo(UserRole.SELLER);
        }
        // 주문 검증
        @Test
        @DisplayName("주문 상태를 검증합니다.")
        void validateOrder_Success(Order order) {
            // given
            User user = helper.getUser();
            // when
            // then
        }
        // 판매자 검증
        // 정산 취소 및 환불 계산



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
    @Test
    @DisplayName("주문 상태가 주문 완료가 아닙니다.")
    void orderStatusFailure_NotCompleted() {
        // given
        Order order = helper.getOrder(helper.getUser());
        order.setOrderStatus(OrderStatus.PENDING);
//        when(order.getOrderStatus()).thenReturn(OrderStatus.PENDING);
        // when
        // then
        assertThrows(BusinessException.class, () -> settlementService.createSettlement(order));
    }

    @Test
    @DisplayName("배송 상태가 배송중입니다.")
    void deliveryStatusFailure_Delivering() {
        // given
        Order order = helper.getOrder(helper.getUser());
        order.setDeliveryStatus(DeliveryStatus.DELIVERING);
        // when
        // then
        assertThrows(BusinessException.class, () -> settlementService.createSettlement(order));
    }

//    @Test
//    @DisplayName("판매자가 올바르지 않습니다.")
//    void SellerCheckFailure() {
//        // given
//        Order order = helper.getOrder(helper.getUser());
//        User seller = helper.getSeller(helper.getGrade());
//        when(order.getOrderStatus()).thenReturn(OrderStatus.COMPLETED);
//        when(order.getDeliveryStatus()).thenReturn(DeliveryStatus.DELIVERED);
//        // when
//        // then
//        assertThrows(BusinessException.class, () -> settlementService.createSettlement(order));
//    }

    @Test
    @DisplayName("User의 Role이 SELLER가 아닙니다.")
    void roleCheckFailure() {
        Order order = helper.getOrder(helper.getUser());
        User seller = helper.getSeller(helper.getGrade());
        User user = helper.getUser();
        when(user.getRole()).thenReturn(UserRole.SELLER);
        assertThrows(BusinessException.class, () -> settlementService.createSettlement(order));
    }

    // 기존 주문 건인지
    @Test
    @DisplayName("정산 완료된 주문 건입니다.")
    void SettlementCompleted() {
        // given
        Order order = helper.getOrder(helper.getUser());
        User seller = helper.getSeller(helper.getGrade());

        // then
        assertThrows(BusinessException.class, () -> settlementService.createSettlement(order));
    }

    @Test
    @DisplayName("주문이 취소되었습니다.")
    void DeliveryStatusFailure() {
        Order order = helper.getOrder(helper.getUser());
        when(order.getDeliveryStatus()).thenReturn(DeliveryStatus.CANCELLED);
        assertThrows(BusinessException.class, () -> settlementService.createSettlement(order));
    }

    // 주문 취소시 환불 금액 발생
    @Test
    @DisplayName("주문 취소시 환불금액 발생")
    void orderCanceled() {
        // given
        Order order = helper.getOrder(helper.getUser());
        when(order.getDeliveryStatus()).thenReturn(DeliveryStatus.CANCELLED);
        // when

        // then
        assertThrows(BusinessException.class, () -> settlementService.orderCanceled(order));
    }


    // 데이터 생성
    public class helper {
        public static OrderItem getOrderItem(SkuEntity sku) {
            OrderItem orderItem = OrderItem.builder()
                    .sku(sku)
                    .quantity(1L)
                    .price(150000L)
                    .build();
            return orderItem;
        }

        public static ProductCategoryEntity getCategory() {
            ProductCategoryEntity productCategoryEntity = ProductCategoryEntity.builder()
                    .id(1L)
                    .depth(1)
                    .name("원목")
                    .parentId(1L)
                    .build();
            return productCategoryEntity;
        }

        public static ProductEntity getProduct(User seller, ProductCategoryEntity productCategoryEntity) {
            ProductEntity product = ProductEntity.builder()
                    .id(1L)
                    .category(productCategoryEntity)
                    .seller(seller)
                    .name("침대")
                    .brand("리바트")
                    .basePrice(150000)
                    .build();
            return product;
        }

        public static SkuEntity getSkuEntity(ProductEntity productEntity) {
            SkuEntity sku = SkuEntity.builder()
                    .id(1L)
                    .priceAdjustment(10)
                    .stockQuantity(11L)
                    .product(productEntity)
                    .build();
            return sku;
        }

        public static Order getOrder(User user) {
            Order order = com.homesweet.homesweetback.domain.order.entity.Order.builder()
                    .user(user)
                    .orderStatus(OrderStatus.COMPLETED)
                    .deliveryStatus(DeliveryStatus.DELIVERED)
                    .totalAmount(150000L)
                    .build();
            return order;
        }

        public static User getUser() {
            User user = User.builder()
                    .id(13L)
                    .name("chulsoo")
                    .phoneNumber("010-1234-1234")
                    .address("서울시 강남구 논현로 1")
                    .email("chulsoo@gmail.com")
                    .role(UserRole.USER)
                    .build();
            return user;
        }

        public static User getSeller(Grade grade) {
            User seller = User.builder()
                    .id(14L)
                    .name("kildong")
                    .phoneNumber("010-1111-2345")
                    .address("서울시 강남구 역삼로 1")
                    .email("kildonghong@gmail.com")
                    .role(UserRole.SELLER)
                    .grade(grade)
                    .build();
            return seller;
        }

        public static Grade getGrade() {
            Grade grade = Grade.builder()
                    .gradeId(1)
                    .grade("VIP")
                    .feeRate(BigDecimal.valueOf(0.5))
                    .build();
            return grade;
        }
    }
}