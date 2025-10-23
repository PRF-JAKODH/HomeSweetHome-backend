package com.homesweet.homesweetback.domain.product.product.controller;

import com.homesweet.homesweetback.domain.auth.entity.OAuth2UserPrincipal;
import com.homesweet.homesweetback.domain.product.product.controller.request.ProductSortType;
import com.homesweet.homesweetback.domain.product.product.controller.request.ProductUploadRequest;
import com.homesweet.homesweetback.domain.product.product.controller.response.*;
import com.homesweet.homesweetback.domain.product.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.nio.file.attribute.UserPrincipal;
import java.util.List;

/**
 * 제품 컨트롤러
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 21.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService service;

    @PostMapping
    public ResponseEntity<ProductResponse> registerProduct(
            @RequestHeader(value = "X-Test-User-Id", defaultValue = "1") Long sellerId,
            @Valid @ModelAttribute ProductUploadRequest request
    ) {
//
//        OAuth2UserPrincipal principal = (OAuth2UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

//        Long sellerId = principal.getUserId();

        ProductResponse response = service.registerProduct(sellerId, request.product(), request.mainImage(), request.detailImages());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 제품 프리뷰 조회 (스토어 > 제품 조회)
    @GetMapping("/previews")
    public ResponseEntity<ProductScrollResponse> getProductPreviews(
            @RequestParam(required = false) Long cursorId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "LATEST", required = false) ProductSortType sortType
    ) {
        ProductScrollResponse response = service.getProductPreview(cursorId, categoryId, limit, keyword, sortType);

        return ResponseEntity.ok(response);
    }

    // 제품 상세 페이지 조회
    @GetMapping("/{productId}")
    public ResponseEntity<ProductPreviewResponse> getProductDetail(@PathVariable Long productId) {

        ProductPreviewResponse response = service.getProductDetail(productId);

        return ResponseEntity.ok(response);
    }

    // 제품 옵션 별 재고 조회 -> 무옵션, 단일 옵션, 다중 옵션마다 달라야 할까?
    @GetMapping("/{productId}/stocks")
    public ResponseEntity<List<SkuStockResponse>> getProductStock(@PathVariable Long productId) {
        List<SkuStockResponse> response = service.getProductStock(productId);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<ProductManageResponse>> getSellerProducts(
            @RequestHeader(value = "X-Test-User-Id", defaultValue = "1") Long sellerId
    ) {
//        OAuth2UserPrincipal principal = (OAuth2UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
//        Long sellerId = principal.getUserId();

        List<ProductManageResponse> response = service.getSellerProducts(sellerId);
        return ResponseEntity.ok(response);
    }
}
