package com.homesweet.homesweetback.domain.search.community.controller;

import com.homesweet.homesweetback.domain.search.community.service.CommunitySearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
