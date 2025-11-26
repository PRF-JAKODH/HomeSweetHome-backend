package com.homesweet.homesweetback.domain.search.community.service;

import java.util.List;

/**
 * 커뮤니티 검색 서비스
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 26.
 */
public interface CommunitySearchService {

    List<String> autocomplete(String keyword);
}
