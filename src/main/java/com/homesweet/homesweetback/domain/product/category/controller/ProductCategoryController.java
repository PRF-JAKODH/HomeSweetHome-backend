package com.homesweet.homesweetback.domain.product.category.controller;

import com.homesweet.homesweetback.domain.product.category.controller.api.ProductCategoryApi;
import com.homesweet.homesweetback.domain.product.category.controller.request.CategoryCreateRequest;
import com.homesweet.homesweetback.domain.product.category.controller.response.CategoryResponse;
import com.homesweet.homesweetback.domain.product.category.service.ProductCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 제품 카테고리 컨트롤러
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 21.
 */
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class ProductCategoryController implements ProductCategoryApi {

    private final ProductCategoryService service;

    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(
            @Valid @RequestBody CategoryCreateRequest request) {

        CategoryResponse response = service.createCategory(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/parent/{parentId}")
    public ResponseEntity<List<CategoryResponse>> getCategoriesByParentId(@PathVariable Long parentId) {
        List<CategoryResponse> responses = service.getCategoriesByParentId(parentId);

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/top")
    public ResponseEntity<List<CategoryResponse>> getTopLevelCategories() {
        List<CategoryResponse> responses = service.getTopLevelCategories();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/hierarchy/{categoryId}")
    public ResponseEntity<List<CategoryResponse>> getCategoryHierarchy(@PathVariable Long categoryId) {
        List<CategoryResponse> responses = service.getCategoryHierarchy(categoryId);
        return ResponseEntity.ok(responses);
    }
}
