package com.homesweet.homesweetback.domain.product.product.command.service;

import com.homesweet.homesweetback.domain.product.product.command.controller.response.ProductDetailResponse;
import com.homesweet.homesweetback.domain.product.product.command.controller.response.RecentViewPreviewResponse;

import java.util.List;

/**
 * 최근 본 상품
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 13.
 */
public interface RecentViewService {
    void saveView(Long userId, Long productId);
    void cacheDetail(Long productId, ProductDetailResponse detail);

    List<Long> getRecentViewsIds(Long userId);
    RecentViewPreviewResponse getCachedPreview(Long productId);

    void deleteOne(Long userId, Long productId);
}
