package com.dartcodes.smartorder.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.dartcodes.smartorder.model.Product;
import com.dartcodes.smartorder.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductRepository productRepository;

    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category) {
        if (search != null && !search.isEmpty()) {
            return ResponseEntity.ok(productRepository.findByNameContainingIgnoreCase(search));
        }
        if (category != null && !category.isEmpty()) {
            return ResponseEntity.ok(productRepository.findByCategory(category));
        }
        return ResponseEntity.ok(productRepository.findByIsActiveTrue());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable java.util.UUID id) {
        return productRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
