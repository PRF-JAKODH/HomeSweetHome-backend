package com.homesweet.homesweetback.domain.product.recent.controller;

import com.homesweet.homesweetback.domain.auth.entity.OAuth2UserPrincipal;
import com.homesweet.homesweetback.domain.product.product.command.controller.response.RecentViewPreviewResponse;
import com.homesweet.homesweetback.domain.product.recent.service.RecentViewService;
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

    private final RecentViewService recentViewService;

    /**
     * 최근 본 상품 조회 (최신순)
     */
    @GetMapping
    public ResponseEntity<List<RecentViewPreviewResponse>> getRecentViews(
            @AuthenticationPrincipal OAuth2UserPrincipal principal
    ) {
        Long userId = principal.getUserId();

        // 1. 최근 본 상품 ID 목록 가져오기
        List<Long> ids = recentViewService.getRecentViewsIds(userId);

        // 2. Preview 캐시에서 가져오기
        List<RecentViewPreviewResponse> previews = ids.stream()
                .map(recentViewService::getCachedPreview)
                .filter(x -> x != null)
                .toList();

        return ResponseEntity.ok(previews);
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

}
