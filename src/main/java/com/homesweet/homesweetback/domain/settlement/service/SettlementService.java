package com.homesweet.homesweetback.domain.settlement.service;

import com.homesweet.homesweetback.common.exception.BusinessException;
import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.grade.service.GradeService;
import com.homesweet.homesweetback.domain.order.entity.DeliveryStatus;
import com.homesweet.homesweetback.domain.order.entity.Order;
import com.homesweet.homesweetback.domain.settlement.util.ExtractedSeller;
import com.homesweet.homesweetback.domain.settlement.util.SettlementCalculater;
import com.homesweet.homesweetback.domain.settlement.dto.response.SettlementResponse;
import com.homesweet.homesweetback.domain.settlement.entity.Settlement;
import com.homesweet.homesweetback.domain.settlement.repository.SettlementRepository;
import com.homesweet.homesweetback.domain.settlement.validation.SettlementValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementService {
    private final SettlementRepository settlementRepository;
    private final GradeService gradeService;
    private final SettlementValidator settlementValidator;
    private final SettlementCalculater settlementCalculater;
    private final ExtractedSeller extractedSeller;
    // 정산 생성 스케줄러


    // TODO: 결제에서 받지말고 한번에 처리하게끔 구조를 변경
    // 주문 확정(결제 완료)시 정산 생성
    @Transactional
    public void createSettlement(Order order) {
//        ZoneId KST = ZoneId.of("Asia/Seoul");
//        LocalDateTime cutoffTime = LocalDateTime.now(KST);
//        List<Order> unsettledOrders = settlementRepository.findUnSettlementOrders(OrderStatus.COMPLETED, cutoffTime);
//
//        // 검증
//        settlementValidator.validateUnsettledOrders(unsettledOrders);

        // 검증
        settlementValidator.validateOrder(order);

        // 판매자 정보 가져오기
        User seller = extractedSeller.extractSeller(order);
        // 검증
        settlementValidator.validateSeller(seller);

        //TODO: 계산 로직만 메서드를 분리한다면 순수하게 테스트 가능
        // 4. 정산 금액 계산(메소드 분리)
        SettlementCalculater.Result result = settlementCalculater.getResult(order, seller);

        // 5. 계산된 금액 저장
        Settlement settlement = Settlement.builder()
                .order(order)
                .salesAmount(result.totalAmount())
                .fee(result.fee())
                .vat(result.vat())
                .refundAmount(result.refundAmount())
                .settlementAmount(result.settlementAmount())
                .settlementDate(LocalDateTime.now())
                .settlementStatus("PENDING")
                .userId(seller.getId())
                .build();

        settlementRepository.save(settlement);
    }

    // 전체 주문건별 정산내역 상태별 조회(기간 + 상태)
    @Transactional(readOnly = true)
    public Page<SettlementResponse> getSettlementStatusList(Long userId, LocalDateTime startDate, LocalDateTime endDate, String settlementStatus, Pageable pageable) {
        // 1. 정산 상태값 전체 -> 필터링 X
        String status = (settlementStatus == null) ? null : settlementStatus.trim();
        if("all".equalsIgnoreCase(status)) {
            status = null;
        }
        // 2. 날짜 범위 경계
        LocalDateTime start = startDate.toLocalDate().atStartOfDay();
        LocalDateTime end = endDate.toLocalDate().plusDays(1).atStartOfDay();

        // 3. 전 후 날짜 확인
        if(start.isAfter(end)){
            throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);
        }
        return settlementRepository.findBySettlement(userId, start, end, status, pageable);
    }
    // 주문 취소시 환불 금액 반영 및 정산 금액 변경
    @Transactional
    public void orderCanceled(Order order) {
        Long orderId = order.getId();

        // 1. 정산 데이터 확인
        Settlement settlement = settlementRepository.findByOrderId(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND));

        // 2. 이미 취소된 정산인지 확인
        if("CANCELED".equalsIgnoreCase(settlement.getSettlementStatus())){
            return;
        }
        // 3. 배송 상태가 주문 취소인지 확인
        if (settlement.getOrder().getDeliveryStatus() != DeliveryStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS);
        }
        // 4. 환불금액 계산 (판매금액 + 부가세 - 수수료)
        BigDecimal saleAmount = settlement.getSalesAmount();
        BigDecimal fee = settlement.getFee();
        BigDecimal vat = settlement.getVat();
        BigDecimal refundAmount = saleAmount.add(vat).subtract(fee).max(BigDecimal.ZERO);

        // 5. 환불 금액 반영 및 정산 금액 재계산
        int updated = settlementRepository.applyRefundAmount(orderId, refundAmount);

        if(updated != 1){
            throw new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND);
        }
    }
}