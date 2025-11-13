package com.homesweet.homesweetback.domain.settlement.service;

import com.homesweet.homesweetback.common.exception.BusinessException;
import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.order.entity.DeliveryStatus;
import com.homesweet.homesweetback.domain.order.entity.Order;
import com.homesweet.homesweetback.domain.settlement.data.HelperData;
import com.homesweet.homesweetback.domain.settlement.entity.Settlement;
import com.homesweet.homesweetback.domain.settlement.repository.SettlementRepository;
import com.homesweet.homesweetback.domain.settlement.repository.querydsl.CustomSettlementRepository;
import com.homesweet.homesweetback.domain.settlement.util.SettlementCalculator;
import com.homesweet.homesweetback.domain.settlement.validation.SettlementValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("정산 취소 테스트")
public class SettlementCanceledTest {
    @InjectMocks
    private SettlementService settlementService;
    @Mock
    private SettlementValidator settlementValidator;
    @Mock
    private SettlementRepository settlementRepository;
    @Mock
    private SettlementCalculator settlementCalculator;
    @Mock
    private CustomSettlementRepository customSettlementRepository;

    // 주문 건별 정산내역 조회 및 상태별 조회
    // 정산 취소 및 환불 계산
    @Test
    @DisplayName("주문 취소시 환불 금액이 반영되고 정산 금액이 변경된다.")
    void orderCanceled_Success() {
        // given
        Order order = HelperData.getOrder(HelperData.getUser());
        order.setDeliveryStatus(DeliveryStatus.CANCELLED);
        ReflectionTestUtils.setField(order, "id", 1L);
        Settlement settlement = HelperData.getSettlement();

        assertThat(settlement).isNotNull();
        given(settlementRepository.findByOrderId(1L)).willReturn(Optional.of(settlement));
        given(settlementCalculator.refundResult(any(Settlement.class))).willReturn(BigDecimal.ZERO);
        given(customSettlementRepository.applyRefundAmount(anyLong(), any(BigDecimal.class))).willReturn(1);

        // when
        assertThatCode(() -> settlementService.orderCanceled(order)).doesNotThrowAnyException();
        // then
        then(settlementValidator).should().validateCanceled(settlement, order);
        then(customSettlementRepository).should(times(1)).applyRefundAmount(anyLong(), any(BigDecimal.class));
    }



    @Nested
    @DisplayName("실패 케이스")
    class OrderCanceled_Failure {
        @Test
        @DisplayName("정산 데이터가 존재하지 않으면 예외 발생")
        void orderCanceled_Failure_NotFound() {
            Order order = HelperData.getOrder(HelperData.getUser());
            ReflectionTestUtils.setField(order, "id", 1L);

            given(settlementRepository.findByOrderId(1L)).willReturn(Optional.empty());
            assertThatThrownBy(() -> settlementService.orderCanceled(order))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ErrorCode.SETTLEMENT_NOT_FOUND.getMessage());

            then(settlementRepository).should(times(1)).findByOrderId(anyLong());

        }

        // 주문 취소시 환불 금액 발생
        @Test
        @DisplayName("배송 상태 취소시 예외 발생")
        void orderCanceled() {
            // given
            Order order = HelperData.getOrder(HelperData.getUser());
            ReflectionTestUtils.setField(order, "id", 1L);
            order.setDeliveryStatus(DeliveryStatus.DELIVERING);
            Settlement settlement = HelperData.getSettlement();
            given(settlementRepository.findByOrderId(anyLong())).willReturn(Optional.of(settlement));

            doThrow(new BusinessException(ErrorCode.INVALID_ORDER_STATUS))
                    .when(settlementValidator)
                    .validateCanceled(any(Settlement.class), any(Order.class));

            assertThatThrownBy(() -> settlementService.orderCanceled(order))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ErrorCode.INVALID_ORDER_STATUS.getMessage());
        }
    }

    @Test
    @DisplayName("환불 금액 update 실패시 예외 발생")
    void orderCanceled_Update_Failure() {
        Order order = HelperData.getOrder(HelperData.getUser());
        ReflectionTestUtils.setField(order, "id", 1L);
        Settlement settlement = HelperData.getSettlement();
        given(settlementRepository.findByOrderId(1L)).willReturn(Optional.of(settlement));
        given(settlementCalculator.refundResult(any(Settlement.class))).willReturn(BigDecimal.ZERO);
        given(customSettlementRepository.applyRefundAmount(anyLong(), any(BigDecimal.class))).willReturn(0);

        assertThatThrownBy(() -> settlementService.orderCanceled(order))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.SETTLEMENT_NOT_FOUND.getMessage());
    }
}
