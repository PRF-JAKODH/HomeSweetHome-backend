package com.homesweet.homesweetback.domain.settlement.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record SettlementRequest(
        @NotBlank(message = "판매자 번호는 필수입니다.")
        Long userId,
        LocalDate startDate,
        LocalDate endDate
) {
}
