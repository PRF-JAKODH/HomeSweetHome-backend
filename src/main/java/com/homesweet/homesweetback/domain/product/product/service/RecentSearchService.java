package com.homesweet.homesweetback.domain.product.product.service;

import java.util.List;

/**
 * 최근 검색 조회 서비스
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 13.
 */
public interface RecentSearchService {

    void save(Long userId, String keyword);

    List<String> getRecent(Long userId);

    void deleteKeyword(Long userId, String keyword);

    void clearAll(Long userId);

}
