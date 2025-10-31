package com.homesweet.homesweetback.domain.product.cart.controller;

import com.homesweet.homesweetback.common.util.ScrollResponse;
import com.homesweet.homesweetback.domain.auth.entity.OAuth2UserPrincipal;
import com.homesweet.homesweetback.domain.product.cart.controller.request.CartCountResponse;
import com.homesweet.homesweetback.domain.product.cart.controller.request.CartRequest;
import com.homesweet.homesweetback.domain.product.cart.controller.request.DeleteCartItemsRequest;
import com.homesweet.homesweetback.domain.product.cart.controller.response.CartResponse;
import com.homesweet.homesweetback.domain.product.cart.domain.Cart;
import com.homesweet.homesweetback.domain.product.cart.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
            @AuthenticationPrincipal OAuth2UserPrincipal principal,
            @RequestBody CartRequest request
    ) {

        Long userId = principal.getUserId();

        Cart response = service.addToCart(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<ScrollResponse<CartResponse>> getCartItems(
            @AuthenticationPrincipal OAuth2UserPrincipal principal,
            @RequestParam(required = false) Long cursorId,
            @RequestParam(defaultValue = "10") int size
    ) {
        Long userId = principal.getUserId();

        ScrollResponse<CartResponse> response = service.getCartItems(userId, cursorId, size);

        return ResponseEntity.ok(response);

    }

    @GetMapping("/count")
    public ResponseEntity<CartCountResponse> getCartItemCount(
            @AuthenticationPrincipal OAuth2UserPrincipal principal
    ) {
        Long userId = principal.getUserId();

        int count = service.getCartItemCount(userId);
        return ResponseEntity.ok(new CartCountResponse(count));
    }

    @DeleteMapping("/{cartId}")
    public ResponseEntity<Void> deleteCartItem(
            @PathVariable Long cartId,
            @AuthenticationPrincipal OAuth2UserPrincipal principal
    ) {
        Long userId = principal.getUserId();
        service.deleteCartItem(userId, cartId);
        return null;
    }

    // 선택한 장바구니 제품 모두 제거
    @DeleteMapping("/batch")
    public ResponseEntity<Void> deleteSelectedCartItems(
            @AuthenticationPrincipal OAuth2UserPrincipal principal,
            @RequestBody DeleteCartItemsRequest request
    ) {
        Long userId = principal.getUserId();
        service.deleteSelectedCartItems(userId, request.cartIds());
        return ResponseEntity.noContent().build();
    }
}
