package com.homesweet.homesweetback.domain.product.category.service.impl;

import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.common.valid.ProductValidator;
import com.homesweet.homesweetback.domain.product.category.controller.request.CategoryCreateRequest;
import com.homesweet.homesweetback.domain.product.category.controller.response.CategoryResponse;
import com.homesweet.homesweetback.domain.product.category.domain.ProductCategory;
import com.homesweet.homesweetback.domain.product.category.domain.exception.ProductCategoryException;
import com.homesweet.homesweetback.domain.product.category.repository.ProductCategoryRepository;
import com.homesweet.homesweetback.domain.product.category.service.ProductCategoryService;
import com.homesweet.homesweetback.domain.product.category.service.cache.CacheCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
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

    private final ProductValidator validator;
    private final ProductCategoryRepository repository;
    private final CacheCategory cacheCategory;

    @Override
    @Transactional
    @CacheEvict(value = {
            "topLevelCategories",
            "categoriesByParent",
            "categoryHierarchy",
            "getCategoryById"
    }, allEntries = true)
    public CategoryResponse createCategory(CategoryCreateRequest request) {

        validator.validateDuplicateCategoryName(request.name());

        int depth = 0;
        if (!request.isParentIdNull()) {
            ProductCategory parent = repository.findById(request.parentId())
                    .orElseThrow(() -> new ProductCategoryException(ErrorCode.CANNOT_FOUND_PARENT_CATEGORY_ERROR));

            depth = parent.depth() + 1;

            parent.validateMaxDepth(depth);
        }

        ProductCategory category = ProductCategory.createCategory(request.name(), request.parentId(), depth);

        ProductCategory domain = repository.save(category);

        cacheCategory.evictAllCategoryCaches();

        return CategoryResponse.from(domain);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "getCategoryById", key = "#categoryId")
    public ProductCategory getCategoryById(Long categoryId) {
        return repository.findById(categoryId)
                .orElseThrow(() -> new ProductCategoryException(ErrorCode.CANNOT_FOUND_CATEGORY_ERROR));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "categoriesByParent", key = "#parentId")
    public List<CategoryResponse> getCategoriesByParentId(Long parentId) {
        return repository.findByParentId(parentId).stream()
                .map(CategoryResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "topLevelCategories")
    public List<CategoryResponse> getTopLevelCategories() {
        return repository.findTopLevelCategories().stream()
                .map(CategoryResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "categoryHierarchy", key = "#categoryId")
    public List<CategoryResponse> getCategoryHierarchy(Long categoryId) {
        ProductCategory category = repository.findById(categoryId)
                .orElseThrow(() -> new ProductCategoryException(ErrorCode.CANNOT_FOUND_CATEGORY_ERROR));

        List<CategoryResponse> hierarchy = new ArrayList<>();

        while (category != null) {
            hierarchy.add(CategoryResponse.from(category));

            if (category.parentId() == null) {
                break;
            }

            category = repository.findById(category.parentId()).orElse(null);
        }

        Collections.reverse(hierarchy);

        return hierarchy;
    }
}
