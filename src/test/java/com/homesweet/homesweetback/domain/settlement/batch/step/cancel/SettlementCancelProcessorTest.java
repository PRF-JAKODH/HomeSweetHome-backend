package com.homesweet.homesweetback.domain.settlement.batch.step.cancel;

import com.homesweet.homesweetback.common.exception.BusinessException;
import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.grade.service.GradeService;
import com.homesweet.homesweetback.domain.order.entity.DeliveryStatus;
import com.homesweet.homesweetback.domain.order.entity.Order;
import com.homesweet.homesweetback.domain.settlement.data.HelperData;
import com.homesweet.homesweetback.domain.settlement.entity.Settlement;
import com.homesweet.homesweetback.domain.settlement.repository.SettlementRepository;
import com.homesweet.homesweetback.domain.settlement.util.calculator.SettlementCalculator;
import com.homesweet.homesweetback.domain.settlement.validation.SettlementValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("정산 취소 processor 단위테스트")
class SettlementCancelProcessorTest {
    @Mock
    private SettlementRepository settlementRepository;

    @InjectMocks
    private SettlementCancelProcessor processor;

    @Mock
    private GradeService gradeService;

    @Mock
    private SettlementValidator settlementValidator;

    @Mock
    private SettlementCalculator settlementCalculator;

    @Nested
    @DisplayName("성공 케이스")
    class Success {
        @Test
        @DisplayName("정산 취소 성공")
        void process_success() {

            Order order = HelperData.getOrder(HelperData.getUser());
            order.setDeliveryStatus(DeliveryStatus.CANCELLED);

            Settlement settlement = HelperData.getSettlementWithDate(LocalDate.now());
            settlement.setSettlementStatus("PENDING");

            given(settlementRepository.findByOrderId(order.getId()))
                    .willReturn(Optional.of(settlement));

            doCallRealMethod().when(settlementValidator).validateCanceled(settlement, order);

            given(settlementCalculator.refundResult(settlement))
                    .willReturn(BigDecimal.valueOf(5000));

            Settlement result = processor.process(order);

            assertThat(result.getSettlementStatus()).isEqualTo("CANCELED");
            assertThat(result.getRefundAmount()).isEqualTo(BigDecimal.valueOf(5000));
        }

    }

    @Nested
    @DisplayName("실패 케이스")
    class Failure {
        @Test
        @DisplayName("정산 데이터가 존재하지 않으면 BusinessException 발생")
        void process_fail_noSettlement() {

            Order order = HelperData.getOrder(HelperData.getUser());

            given(settlementRepository.findByOrderId(order.getId()))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> processor.process(order))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ErrorCode.SETTLEMENT_NOT_FOUND.getMessage());

            verify(settlementRepository).findByOrderId(order.getId());
            verify(settlementValidator, never()).validateCanceled(any(), any());
        }

        @Test
        @DisplayName("정산 상태가 이미 CANCELED 이면 예외 발생")
        void process_fail_alreadyCanceled() {

            Order order = HelperData.getOrder(HelperData.getUser());
            order.setDeliveryStatus(DeliveryStatus.CANCELLED); // validator 2단계 통과

            Settlement settlement = HelperData.getSettlementWithDate(LocalDate.now());
            settlement.setSettlementStatus("CANCELED"); // 실패 포인트

            given(settlementRepository.findByOrderId(order.getId()))
                    .willReturn(Optional.of(settlement));

            // validator 실제 로직 실행
            doCallRealMethod().when(settlementValidator).validateCanceled(any(), any());

            assertThatThrownBy(() -> processor.process(order))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ErrorCode.ALREADY_SETTLEMENT_CANCELED.getMessage());
        }

        @Test
        @DisplayName("주문 배송상태가 CANCELLED 가 아니면 예외 발생")
        void process_fail_invalidOrderStatus() {

            Order order = HelperData.getOrder(HelperData.getUser());
            order.setDeliveryStatus(DeliveryStatus.DELIVERED); // 실패 포인트

            Settlement settlement = HelperData.getSettlementWithDate(LocalDate.now());
            settlement.setSettlementStatus("PENDING");

            given(settlementRepository.findByOrderId(order.getId()))
                    .willReturn(Optional.of(settlement));

            doCallRealMethod().when(settlementValidator).validateCanceled(any(), any());

            assertThatThrownBy(() -> processor.process(order))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ErrorCode.INVALID_ORDER_STATUS.getMessage());
        }
    }
}