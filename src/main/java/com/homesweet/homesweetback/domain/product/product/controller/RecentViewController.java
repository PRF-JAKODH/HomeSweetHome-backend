package com.homesweet.homesweetback.domain.product.product.controller;

import com.homesweet.homesweetback.domain.auth.entity.OAuth2UserPrincipal;
import com.homesweet.homesweetback.domain.product.product.controller.response.ProductDetailResponse;
import com.homesweet.homesweetback.domain.product.product.service.ProductSearchService;
import com.homesweet.homesweetback.domain.product.product.service.ProductService;
import com.homesweet.homesweetback.domain.product.product.service.RecentViewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 최근 본 상품 컨트롤러
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 13.
 */
@RestController
@RequestMapping("/api/v1/recent-view")
@RequiredArgsConstructor
public class RecentViewController {

    private final ProductSearchService productSearchService;
    private final RecentViewService recentViewService;

    @GetMapping("/{productId}")
    public ResponseEntity<ProductDetailResponse> getProductDetail(
            @AuthenticationPrincipal OAuth2UserPrincipal principal,
            @PathVariable Long productId) {

        Long userId = principal.getUserId();

        ProductDetailResponse response = productSearchService.getProductDetail(userId, productId);

        return ResponseEntity.ok(response);
    }

    /**
     * 최근 본 상품 조회 (최신순)
     */
    @GetMapping
    public ResponseEntity<List<Long>> getRecentViews(
            @AuthenticationPrincipal OAuth2UserPrincipal principal
    ) {
        List<Long> views = recentViewService.getRecentViews(principal.getUserId());
        return ResponseEntity.ok(views);
    }

    /**
     * 특정 상품 삭제
     */
    @DeleteMapping
    public ResponseEntity<Void> deleteOne(
            @AuthenticationPrincipal OAuth2UserPrincipal principal,
            @RequestParam Long productId
    ) {
        recentViewService.deleteOne(principal.getUserId(), productId);
        return ResponseEntity.ok().build();
    }

    /**
     * 전체 삭제
     */
    @DeleteMapping("/all")
    public ResponseEntity<Void> clearAll(
            @AuthenticationPrincipal OAuth2UserPrincipal principal
    ) {
        recentViewService.clearAll(principal.getUserId());
        return ResponseEntity.ok().build();
    }

}
