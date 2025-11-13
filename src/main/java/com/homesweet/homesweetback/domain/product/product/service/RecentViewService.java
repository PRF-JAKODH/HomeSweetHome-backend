package com.homesweet.homesweetback.domain.product.product.service;

import java.util.List;

/**
 * 최근 본 상품
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 13.
 */
public interface RecentViewService {
    void saveView(Long userId, Long productId);
    List<Long> getRecentViews(Long userId);
    void deleteOne(Long userId, Long productId);
    void clearAll(Long userId);
}
