package com.homesweet.homesweetback.domain.settlement.util.vo;

import com.homesweet.homesweetback.domain.settlement.entity.Settlement;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class DailyTotals {
    private BigDecimal totalSales;
    private BigDecimal totalFee;
    private BigDecimal totalVat;
    private BigDecimal totalRefund;
    private BigDecimal totalSettlement;

    public static DailyTotals empty(){
        return new DailyTotals(
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );
    }

//    public void add(Settlement s){
//        BigDecimal refund = s.getRefundAmount() == null ? BigDecimal.ZERO : s.getRefundAmount();
//
//        totalSales = s.getSalesAmount().add(totalSales);
//        totalFee = s.getFee().add(totalFee);
//        totalVat = s.getVat().add(totalVat);
//        totalRefund = s.getRefundAmount().add(refund);
//        totalSettlement = s.getSettlementAmount().add(totalSettlement);
//    }


    public void add(Settlement s){
        totalSales      = totalSales.add(safe(s.getSalesAmount()));
        totalFee        = totalFee.add(safe(s.getFee()));
        totalVat        = totalVat.add(safe(s.getVat()));
        totalRefund     = totalRefund.add(safe(s.getRefundAmount()));
        totalSettlement = totalSettlement.add(safe(s.getSettlementAmount()));
    }

    private BigDecimal safe(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

}
