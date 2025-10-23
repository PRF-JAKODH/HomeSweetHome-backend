package com.homesweet.homesweetback.domain.product.product.controller;

import com.homesweet.homesweetback.domain.product.product.controller.request.ProductSortType;
import com.homesweet.homesweetback.domain.product.product.controller.request.ProductUploadRequest;
import com.homesweet.homesweetback.domain.product.product.controller.response.ProductResponse;
import com.homesweet.homesweetback.domain.product.product.controller.response.ProductScrollResponse;
import com.homesweet.homesweetback.domain.product.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/previews")
    public ResponseEntity<ProductScrollResponse> getProductPreviews(
            @RequestParam(required = false) Long cursorId,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "LATEST", required = false) ProductSortType sortType
    ) {
        ProductScrollResponse response = service.getProductPreview(cursorId, size, keyword, sortType);

        return ResponseEntity.ok(response);
    }
}
