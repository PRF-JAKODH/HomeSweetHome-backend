package com.homesweet.homesweetback.domain.product.cart.controller.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record CartQuantityUpdateRequest(
        @Max(value = 10, message = "최대 10개까지 담을 수 있습니다.")
        @Min(value = 1, message = "수량은 1개 이상이어야 합니다.")
        int quantity
) {}