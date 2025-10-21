package com.homesweet.homesweetback.domain.product.category.controller;

import com.homesweet.homesweetback.domain.product.category.controller.api.ProductCategoryApi;
import com.homesweet.homesweetback.domain.product.category.controller.request.CategoryCreateRequest;
import com.homesweet.homesweetback.domain.product.category.controller.response.CategoryResponse;
import com.homesweet.homesweetback.domain.product.category.service.ProductCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 제품 카테고리 컨트롤러
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 21.
 */
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class ProductCategoryController implements ProductCategoryApi {

    private final ProductCategoryService service;

    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(
            @Valid @RequestBody CategoryCreateRequest request) {

        CategoryResponse response = service.createCategory(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
