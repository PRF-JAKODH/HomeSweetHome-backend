package com.homesweet.homesweetback.domain.search.community.repository;

import java.util.List;

/**
 * 게시글 검색 레포지토리
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 26.
 */
public interface CommunityPostRepository {

    List<String> autocomplete(String keyword);

}
