package com.homesweet.homesweetback.domain.product.cart.controller;

import com.homesweet.homesweetback.domain.product.cart.controller.request.CartRequest;
import com.homesweet.homesweetback.domain.product.cart.domain.Cart;
import com.homesweet.homesweetback.domain.product.cart.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 장바구니 컨트롤러
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 24.
 */
@RestController
@RequestMapping("/api/v1/carts")
@RequiredArgsConstructor
public class CartController {

    private final CartService service;

    @PostMapping
    public ResponseEntity<Cart> addToCart(
            @RequestHeader(value = "X-Test-User-Id", defaultValue = "1") Long userId, // 테스트 용
            @RequestBody CartRequest request
    ) {
        Cart response = service.addToCart(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
