package com.homesweet.homesweetback.domain.product.category.service.impl;

import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.product.category.controller.request.CategoryCreateRequest;
import com.homesweet.homesweetback.domain.product.category.controller.response.CategoryResponse;
import com.homesweet.homesweetback.domain.product.category.domain.ProductCategory;
import com.homesweet.homesweetback.domain.product.category.domain.exception.ProductCategoryException;
import com.homesweet.homesweetback.domain.product.category.repository.ProductCategoryRepository;
import com.homesweet.homesweetback.domain.product.category.service.ProductCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 상품 카테고리 서비스 구현 코드
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 21.
 */
@Service
@RequiredArgsConstructor
public class ProductCategoryServiceImpl implements ProductCategoryService {

    private final ProductCategoryRepository repository;

    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryCreateRequest request) {

        repository.findByName(request.name())
                .ifPresent(c -> {
                    throw new ProductCategoryException(ErrorCode.DUPLICATED_CATEGORY_NAME_ERROR);
                });

        int depth = 0;
        if (request.parentId() != null) {
            ProductCategory parent = repository.findById(request.parentId())
                    .orElseThrow(() -> new ProductCategoryException(ErrorCode.CANNOT_FOUND_PARENT_CATEGORY_ERROR));

            depth = parent.depth() + 1;

            if (depth > ProductCategory.MAX_DEPTH) {
                throw new ProductCategoryException(ErrorCode.CATEGORY_DEPTH_EXCEEDED_ERROR);
            }
        }

        ProductCategory category = ProductCategory.createCategory(request.name(), request.parentId(), depth);

        ProductCategory domain = repository.save(category);

        return CategoryResponse.from(domain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategoriesByParentId(Long parentId) {
        return repository.findByParentId(parentId).stream()
                .map(CategoryResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getTopLevelCategories() {
        return repository.findTopLevelCategories().stream()
                .map(CategoryResponse::from)
                .toList();
    }
}
