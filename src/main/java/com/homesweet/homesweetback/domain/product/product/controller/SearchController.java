package com.homesweet.homesweetback.domain.product.product.controller;

import com.homesweet.homesweetback.common.util.ScrollResponse;
import com.homesweet.homesweetback.domain.auth.entity.OAuth2UserPrincipal;
import com.homesweet.homesweetback.domain.product.product.controller.request.ProductSortType;
import com.homesweet.homesweetback.domain.product.product.controller.response.ProductPreviewResponse;
import com.homesweet.homesweetback.domain.product.product.service.ProductSearchService;
import com.homesweet.homesweetback.domain.product.product.service.RecentSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    private final RecentSearchService recentSearchService;

    @GetMapping("/search")
    public ResponseEntity<ScrollResponse<ProductPreviewResponse>> search(
            @RequestParam(required = false) Long cursorId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "LATEST", required = false) ProductSortType sortType,
            @AuthenticationPrincipal OAuth2UserPrincipal principal
    ) {

        Long userId = principal.getUserId();

        ScrollResponse<ProductPreviewResponse> response =
                searchService.search(cursorId, categoryId, userId, limit, keyword, sortType);

        return ResponseEntity.ok(response);
    }

    /**
     * 최근 검색어 보기
     */
    @GetMapping("/recent")
    public List<String> recent(@AuthenticationPrincipal OAuth2UserPrincipal principal) {

        Long userId = principal.getUserId();

        return recentSearchService.getRecent(userId);
    }

    /**
     * 특정 검색어 삭제
     */
    @DeleteMapping("/recent/keyword")
    public void deleteKeyword(
            @AuthenticationPrincipal OAuth2UserPrincipal principal,
            @RequestParam String keyword) {

        Long userId = principal.getUserId();

        recentSearchService.deleteKeyword(userId, keyword);
    }

    /**
     * 전체 삭제
     */
    @DeleteMapping("/recent")
    public void clearAll(@AuthenticationPrincipal OAuth2UserPrincipal principal) {

        Long userId = principal.getUserId();

        recentSearchService.clearAll(userId);
    }
}
