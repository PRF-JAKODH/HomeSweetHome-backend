package com.homesweet.homesweetback.domain.product.category.service;

import com.homesweet.homesweetback.domain.product.category.controller.request.CategoryCreateRequest;
import com.homesweet.homesweetback.domain.product.category.controller.response.CategoryResponse;

import java.util.List;

/**
 * 제품 카테고리 서비스 인터페이스
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 21.
 */
public interface ProductCategoryService {

    CategoryResponse createCategory(CategoryCreateRequest request);

    List<CategoryResponse> getCategoriesByParentId(Long parentId);

    List<CategoryResponse> getTopLevelCategories();
}