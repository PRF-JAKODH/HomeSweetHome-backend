package com.homesweet.homesweetback.domain.product.review.controller.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

/**
 * 제품 리뷰 등록 요청 DTO
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 23.
 */
public record ProductReviewCreateRequest(
        @NotNull
        @Min(value = 1, message = "별점은 최소 1점입니다")
        @Max(value = 5, message = "별점은 최대 5점입니다")
        Integer rating,
        String comment,
        MultipartFile image
        ) {}