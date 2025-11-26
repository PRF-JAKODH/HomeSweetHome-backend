package com.homesweet.homesweetback.domain.search.community.controller;

import com.homesweet.homesweetback.common.util.scroll.SearchScrollResponse;
import com.homesweet.homesweetback.domain.auth.entity.OAuth2UserPrincipal;
import com.homesweet.homesweetback.domain.search.community.controller.response.CommunityPostSearchResponse;
import com.homesweet.homesweetback.domain.search.community.controller.response.CommunitySortType;
import com.homesweet.homesweetback.domain.search.community.service.CommunitySearchService;
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
 * 커뮤니티 검색 컨트롤러
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 26.
 */
@RestController
@RequestMapping("/api/v1/search/community")
@RequiredArgsConstructor
public class CommunitySearchController {

    private final CommunitySearchService communitySearchService;

    /**
     * 게시글 검색 자동완성
     */
    @GetMapping("/autocomplete")
    public ResponseEntity<List<String>> autocomplete(@NotNull @RequestParam String keyword) {

        List<String> result = communitySearchService.autocomplete(keyword);

        return ResponseEntity.ok(result);
    }

    /**
     * 게시글 검색 및 조회
     */
    @GetMapping
    public ResponseEntity<SearchScrollResponse<CommunityPostSearchResponse>> searchPosts(
            @RequestParam(required = false) String nextCursor,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "LATEST") CommunitySortType sortType,
            @RequestParam(defaultValue = "20") int limit,
            @AuthenticationPrincipal OAuth2UserPrincipal principal
    ) {
        Long userId = principal != null ? principal.getUserId() : null;

        SearchScrollResponse<CommunityPostSearchResponse> response =
                communitySearchService.search(userId, nextCursor, keyword, limit, sortType);

        return ResponseEntity.ok(response);
    }

}
