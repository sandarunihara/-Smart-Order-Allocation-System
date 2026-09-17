package com.dartcodes.smartorder.service;

import com.dartcodes.smartorder.dto.AddToCartRequest;
import com.dartcodes.smartorder.dto.CartItemResponse;
import com.dartcodes.smartorder.exception.ResourceNotFoundException;
import com.dartcodes.smartorder.model.CartItem;
import com.dartcodes.smartorder.model.Product;
import com.dartcodes.smartorder.model.User;
import com.dartcodes.smartorder.repository.CartItemRepository;
import com.dartcodes.smartorder.repository.ProductRepository;
import com.dartcodes.smartorder.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public List<CartItemResponse> getCart(String userEmail) {
        User user = getUserByEmail(userEmail);
        return cartItemRepository.findByUserId(user.getId()).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public List<CartItemResponse> addToCart(AddToCartRequest request, String userEmail) {
        User user = getUserByEmail(userEmail);
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        var existingOpt = cartItemRepository.findByUserIdAndProductId(user.getId(), product.getId());
        if (existingOpt.isPresent()) {
            CartItem item = existingOpt.get();
            item.setQuantity(item.getQuantity() + request.getQuantity());
            cartItemRepository.save(item);
        } else {
            CartItem item = CartItem.builder()
                    .userId(user.getId())
                    .product(product)
                    .quantity(request.getQuantity())
                    .build();
            cartItemRepository.save(item);
        }
        return getCart(userEmail);
    }

    @Transactional
    public List<CartItemResponse> updateQuantity(UUID productId, int quantity, String userEmail) {
        User user = getUserByEmail(userEmail);
        if (quantity <= 0) {
            cartItemRepository.deleteByUserIdAndProductId(user.getId(), productId);
        } else {
            var existingOpt = cartItemRepository.findByUserIdAndProductId(user.getId(), productId);
            if (existingOpt.isPresent()) {
                CartItem item = existingOpt.get();
                item.setQuantity(quantity);
                cartItemRepository.save(item);
            }
        }
        return getCart(userEmail);
    }

    @Transactional
    public List<CartItemResponse> removeItem(UUID productId, String userEmail) {
        User user = getUserByEmail(userEmail);
        cartItemRepository.deleteByUserIdAndProductId(user.getId(), productId);
        return getCart(userEmail);
    }

    @Transactional
    public void clearCart(String userEmail) {
        User user = getUserByEmail(userEmail);
        cartItemRepository.deleteByUserId(user.getId());
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private CartItemResponse mapToResponse(CartItem item) {
        return CartItemResponse.builder()
                .id(item.getId())
                .product(item.getProduct())
                .quantity(item.getQuantity())
                .build();
    }
}
