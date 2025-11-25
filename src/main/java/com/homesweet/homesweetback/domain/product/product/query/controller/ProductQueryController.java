package com.homesweet.homesweetback.domain.product.product.query.controller;

import com.homesweet.homesweetback.common.util.SearchScrollResponse;
import com.homesweet.homesweetback.domain.auth.entity.OAuth2UserPrincipal;
import com.homesweet.homesweetback.domain.product.product.command.controller.request.ProductSortType;
import com.homesweet.homesweetback.domain.product.product.query.controller.response.ProductPreviewResponse;
import com.homesweet.homesweetback.domain.product.product.query.service.ProductQueryService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 상품 검색 컨트롤러
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 24.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products/search")
public class ProductQueryController {

    private final ProductQueryService productQueryService;

    /**
     * 상품 검색 (무한 스크롤)
     *
     */
    @GetMapping
    public ResponseEntity<SearchScrollResponse<ProductPreviewResponse>> searchProducts(
            @RequestParam(required = false) String nextCursor,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "LATEST") ProductSortType sortType,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false, name = "optionFilters") List<String> optionFilters,
            @AuthenticationPrincipal OAuth2UserPrincipal principal
    ) {

        Long userId = principal.getUserId();

        SearchScrollResponse<ProductPreviewResponse> result = productQueryService.searchProducts(nextCursor, categoryId, keyword, sortType, minPrice, maxPrice, limit, userId, optionFilters);

        return ResponseEntity.ok(result);
    }

    /**
     * 검색어 자동 완성 API
     */
    @GetMapping("/autocomplete")
    public ResponseEntity<List<String>> autocomplete(@NotNull @RequestParam String keyword) {
        List<String> result = productQueryService.autocomplete(keyword);
        return ResponseEntity.ok(result);
    }
}
