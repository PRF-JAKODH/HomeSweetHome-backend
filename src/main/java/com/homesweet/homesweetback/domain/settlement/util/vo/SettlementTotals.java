package com.homesweet.homesweetback.domain.settlement.util.vo;

import com.homesweet.homesweetback.domain.settlement.entity.DailySettlement;
import com.homesweet.homesweetback.domain.settlement.entity.Settlement;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class SettlementTotals {
    private BigDecimal totalSales;
    private BigDecimal totalFee;
    private BigDecimal totalVat;
    private BigDecimal totalRefund;
    private BigDecimal totalSettlement;

    public static SettlementTotals empty(){
        return new SettlementTotals(
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


    // 일별 집계시 사용 (주문건->일별)
//    public void add(Settlement s){
//        totalSales      = totalSales.add(safe(s.getSalesAmount()));
//        totalFee        = totalFee.add(safe(s.getFee()));
//        totalVat        = totalVat.add(safe(s.getVat()));
//        totalRefund     = totalRefund.add(safe(s.getRefundAmount()));
//        totalSettlement = totalSettlement.add(safe(s.getSettlementAmount()));
//    }

    // settlementTotal -> settlementTotal
    public void add(SettlementTotals o){
        if(o == null) return;
        this.totalSales      = this.totalSales.add(o.totalSales);
        this.totalFee        = this.totalFee.add(o.totalFee);
        this.totalVat        = this.totalVat.add(o.totalVat);
        this.totalRefund     = this.totalRefund.add(o.totalRefund);
        this.totalSettlement = this.totalSettlement.add(o.totalSettlement);
    }

    // Settlement → SettlementTotals 변환 (Mapper가 호출)
    public static SettlementTotals from(Settlement s) {
        return new SettlementTotals(
                safe(s.getSalesAmount()),
                safe(s.getFee()),
                safe(s.getVat()),
                safe(s.getRefundAmount()),
                safe(s.getSettlementAmount())
        );
    }

    private static BigDecimal safe(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
    // 주별 집계시 사용
    public void add(DailySettlement d) {
        totalSales      = totalSales.add(safe(d.getTotalSales()));
        totalFee        = totalFee.add(safe(d.getTotalFee()));
        totalVat        = totalVat.add(safe(d.getTotalVat()));
        totalRefund     = totalRefund.add(safe(d.getTotalRefund()));
        totalSettlement = totalSettlement.add(safe(d.getTotalSettlement()));
    }
}
