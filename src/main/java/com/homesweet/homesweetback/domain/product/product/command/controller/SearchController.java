package com.homesweet.homesweetback.domain.product.product.command.controller;

import com.homesweet.homesweetback.common.util.ScrollResponse;
import com.homesweet.homesweetback.domain.auth.entity.OAuth2UserPrincipal;
import com.homesweet.homesweetback.domain.product.product.command.controller.request.ProductSortType;
import com.homesweet.homesweetback.domain.product.product.command.controller.response.ProductDetailResponse;
import com.homesweet.homesweetback.domain.product.product.query.controller.response.ProductPreviewResponse;
import com.homesweet.homesweetback.domain.product.product.command.service.ProductSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 최근 검색어 조회
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 13.
 */
@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final ProductSearchService searchService;

    @GetMapping("/{productId}")
    public ResponseEntity<ProductDetailResponse> getProductDetail(
            @AuthenticationPrincipal OAuth2UserPrincipal principal,
            @PathVariable Long productId) {

        Long userId = principal.getUserId();

        ProductDetailResponse response = searchService.getProductDetail(userId, productId);

        return ResponseEntity.ok(response);
    }
}
