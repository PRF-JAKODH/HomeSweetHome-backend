package com.homesweet.homesweetback.domain.product.product.query.controller;

import com.homesweet.homesweetback.domain.product.product.query.service.ProductQueryService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 상품 검색 컨트롤러
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 24.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products/search")
public class ProductQueryController {

    private final ProductQueryService productQueryService;

    /**
     * 검색어 자동 완성 API
     */
    @GetMapping("/autocomplete")
    public ResponseEntity<List<String>> autocomplete(@NotNull @RequestParam String keyword) {
        List<String> result = productQueryService.autocomplete(keyword);
        return ResponseEntity.ok(result);
    }
}
