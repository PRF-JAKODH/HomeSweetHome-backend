package com.homesweet.homesweetback.domain.settlement.util;

public class SettlementStatusUtil {
    private SettlementStatusUtil() {}

    public static String normalizeStatus(String settlementStatus) {
        if (settlementStatus == null) return null;
        String trimmedStatus = settlementStatus.trim();
        return "all".equalsIgnoreCase(trimmedStatus) ? null : trimmedStatus;
    }
}
