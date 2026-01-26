package com.homesweet.homesweetback.domain.order.dto.request;

import jakarta.validation.constraints.NotBlank;

public record OrderCancelRequest(
        String cancelReason
) {
}