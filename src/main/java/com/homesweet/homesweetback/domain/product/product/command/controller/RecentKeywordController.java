package com.homesweet.homesweetback.domain.product.product.command.controller;

import com.homesweet.homesweetback.domain.auth.entity.OAuth2UserPrincipal;
import com.homesweet.homesweetback.domain.product.product.command.service.RecentSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 최근 검색어 컨트롤러
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 13.
 */
@RestController
@RequestMapping("/api/v1/recent-keyword")
@RequiredArgsConstructor
public class RecentKeywordController {

    private final RecentSearchService recentSearchService;

    /**
     * 최근 검색어 보기
     */
    @GetMapping
    public List<String> recent(@AuthenticationPrincipal OAuth2UserPrincipal principal) {

        Long userId = principal.getUserId();

        return recentSearchService.getRecent(userId);
    }

    /**
     * 특정 검색어 삭제
     */
    @DeleteMapping
    public void deleteKeyword(
            @AuthenticationPrincipal OAuth2UserPrincipal principal,
            @RequestParam String keyword) {

        Long userId = principal.getUserId();

        recentSearchService.deleteKeyword(userId, keyword);
    }

    /**
     * 전체 삭제
     */
    @DeleteMapping("/all")
    public void clearAll(@AuthenticationPrincipal OAuth2UserPrincipal principal) {

        Long userId = principal.getUserId();

        recentSearchService.clearAll(userId);
    }

}
