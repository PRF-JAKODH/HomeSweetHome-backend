package com.homesweet.homesweetback.domain.product.product.controller;

import com.homesweet.homesweetback.domain.auth.entity.OAuth2UserPrincipal;
import com.homesweet.homesweetback.domain.product.product.controller.api.ProductAPI;
import com.homesweet.homesweetback.domain.product.product.controller.request.ProductCreateRequest;
import com.homesweet.homesweetback.domain.product.product.controller.response.ProductResponse;
import com.homesweet.homesweetback.domain.product.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 제품 컨트롤러
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 21.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/product")
@RequiredArgsConstructor
public class ProductController implements ProductAPI {

    private final ProductService service;

    @PostMapping
    public ResponseEntity<ProductResponse> registerProduct(
            @RequestHeader(value = "X-Test-User-Id", defaultValue = "1") Long sellerId,
//            @AuthenticationPrincipal OAuth2UserPrincipal principal,
            @Valid @RequestPart ProductCreateRequest request,
            @RequestBody MultipartFile mainImage,
            @RequestPart(value = "detailImages", required = false) List<MultipartFile> detailImages
            ) {

//        Long sellerId = principal.getUserId();

        ProductResponse response = service.registerProduct(sellerId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
