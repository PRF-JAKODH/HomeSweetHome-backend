package com.homesweet.homesweetback.domain.search.community.service;

import com.homesweet.homesweetback.common.util.scroll.SearchScrollResponse;
import com.homesweet.homesweetback.domain.search.chat.controller.response.ChatRoomSortType;
import com.homesweet.homesweetback.domain.search.community.controller.response.CommunityPostSearchResponse;
import com.homesweet.homesweetback.domain.search.community.controller.response.CommunitySortType;

import java.util.List;

/**
 * 커뮤니티 검색 서비스
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 26.
 */
public interface CommunitySearchService {

    List<String> autocomplete(String keyword);

    SearchScrollResponse<CommunityPostSearchResponse> search(Long userId, String cursor, String keyword, int limit, CommunitySortType sortType);
}
