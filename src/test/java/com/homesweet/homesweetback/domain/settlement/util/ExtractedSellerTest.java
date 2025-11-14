package com.homesweet.homesweetback.domain.settlement.util;

import com.homesweet.homesweetback.common.exception.BusinessException;
import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.entity.UserRole;
import com.homesweet.homesweetback.domain.grade.entity.Grade;
import com.homesweet.homesweetback.domain.order.entity.Order;
import com.homesweet.homesweetback.domain.order.entity.OrderItem;
import com.homesweet.homesweetback.domain.product.category.repository.jpa.entity.ProductCategoryEntity;
import com.homesweet.homesweetback.domain.product.product.repository.jpa.entity.ProductEntity;
import com.homesweet.homesweetback.domain.product.product.repository.jpa.entity.SkuEntity;
import com.homesweet.homesweetback.domain.settlement.data.HelperData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@DisplayName("판매자 추출하는 테스트")
public class ExtractedSellerTest {

    @InjectMocks
    private ExtractedSeller extractedSeller;

    // 판매자 확인
    @Test
    @DisplayName("[성공] 주문에서 판매자 정보를 추출한다.")
    void extractedSeller_Success() {
        // given
        Grade grade = HelperData.getGrade();
        User seller = HelperData.getSeller(grade);
        Order order = mock(Order.class);
        OrderItem orderItem = mock(OrderItem.class);
        ProductEntity productEntity = mock(ProductEntity.class);
        SkuEntity skuEntity = mock(SkuEntity.class);

        given(order.getOrderItems()).willReturn(List.of(orderItem));
        given(orderItem.getSku()).willReturn(skuEntity);
        given(skuEntity.getProduct()).willReturn(productEntity);
        given(productEntity.getSeller()).willReturn(seller);
        // when
        User extractSeller = extractedSeller.extractSeller(order);
        // then
        assertThat(extractSeller).isNotNull();
        assertThat(extractSeller.getRole()).isEqualTo(UserRole.SELLER);
    }

    @Nested
    @DisplayName("실패 케이스")
    class Failure {
        @Test
        @DisplayName("주문이 존재하지 않으면 예외 발생")
        void validateOrder_NotExistOrder_Failure() {
            Order order = mock(Order.class);
            given(order.getOrderItems()).willReturn(null);

            assertThatThrownBy(() -> extractedSeller.extractSeller(order))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ErrorCode.ORDER_ITEMS_EMPTY.getMessage());
        }
        @Test
        @DisplayName("주문이 비어있으면 예외 발생")
        void validateDaily_empty_list_Failure() {
            Order order = mock(Order.class);
            given(order.getOrderItems()).willReturn(Collections.emptyList());

            assertThatThrownBy(() -> extractedSeller.extractSeller(order))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ErrorCode.ORDER_ITEMS_EMPTY.getMessage());
        }
    }

}
