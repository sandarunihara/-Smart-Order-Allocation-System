package com.dartcodes.smartorder.controller;

import com.dartcodes.smartorder.dto.AddToCartRequest;
import com.dartcodes.smartorder.dto.CartItemResponse;
import com.dartcodes.smartorder.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<List<CartItemResponse>> getCart(Principal principal) {
        return ResponseEntity.ok(cartService.getCart(principal.getName()));
    }

    @PostMapping
    public ResponseEntity<List<CartItemResponse>> addToCart(@Valid @RequestBody AddToCartRequest request, Principal principal) {
        return ResponseEntity.ok(cartService.addToCart(request, principal.getName()));
    }

    @PutMapping("/{productId}")
    public ResponseEntity<List<CartItemResponse>> updateQuantity(
            @PathVariable UUID productId,
            @RequestBody Map<String, Integer> body,
            Principal principal) {
        int qty = body.getOrDefault("quantity", 1);
        return ResponseEntity.ok(cartService.updateQuantity(productId, qty, principal.getName()));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<List<CartItemResponse>> removeItem(@PathVariable UUID productId, Principal principal) {
        return ResponseEntity.ok(cartService.removeItem(productId, principal.getName()));
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(Principal principal) {
        cartService.clearCart(principal.getName());
        return ResponseEntity.noContent().build();
    }
}
