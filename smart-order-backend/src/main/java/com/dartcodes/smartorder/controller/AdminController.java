package com.dartcodes.smartorder.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.dartcodes.smartorder.dto.DashboardStats;
import com.dartcodes.smartorder.dto.OrderResponse;
import com.dartcodes.smartorder.dto.StatusUpdateRequest;
import com.dartcodes.smartorder.exception.ResourceNotFoundException;
import com.dartcodes.smartorder.model.Branch;
import com.dartcodes.smartorder.model.BranchInventory;
import com.dartcodes.smartorder.model.Product;
import com.dartcodes.smartorder.repository.BranchInventoryRepository;
import com.dartcodes.smartorder.repository.BranchRepository;
import com.dartcodes.smartorder.repository.ProductRepository;
import com.dartcodes.smartorder.service.DashboardService;
import com.dartcodes.smartorder.service.OrderService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final DashboardService dashboardService;
    private final OrderService orderService;
    private final BranchRepository branchRepository;
    private final ProductRepository productRepository;
    private final BranchInventoryRepository inventoryRepository;

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardStats> getDashboard() {
        return ResponseEntity.ok(dashboardService.getDashboardStats());
    }

    @GetMapping("/orders")
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @PutMapping("/orders/{id}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable UUID id,
            @RequestBody StatusUpdateRequest request) {
        return ResponseEntity.ok(orderService.updateOrderStatus(id, request.getStatus()));
    }

    @GetMapping("/branches")
    public ResponseEntity<List<Branch>> getAllBranches() {
        return ResponseEntity.ok(branchRepository.findAll());
    }

    @PostMapping("/branches")
    public ResponseEntity<Branch> createBranch(@RequestBody Branch branch) {
        Branch savedBranch = branchRepository.save(branch);
        List<Product> products = productRepository.findAll();
        for (Product p : products) {
            inventoryRepository.save(BranchInventory.builder()
                    .branchId(savedBranch.getId())
                    .productId(p.getId())
                    .stockQuantity(25)
                    .build());
        }
        return ResponseEntity.ok(savedBranch);
    }

    @PutMapping("/branches/{id}")
    public ResponseEntity<Branch> updateBranch(@PathVariable UUID id, @RequestBody Branch branchData) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
        branch.setName(branchData.getName());
        branch.setAddress(branchData.getAddress());
        branch.setLatitude(branchData.getLatitude());
        branch.setLongitude(branchData.getLongitude());
        branch.setMaxWorkload(branchData.getMaxWorkload());
        branch.setIsActive(branchData.getIsActive());
        return ResponseEntity.ok(branchRepository.save(branch));
    }

    @DeleteMapping("/branches/{id}")
    public ResponseEntity<Void> deleteBranch(@PathVariable UUID id) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
        branch.setIsActive(false);
        branchRepository.save(branch);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/products")
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(productRepository.findAll());
    }

    @PostMapping("/products")
    public ResponseEntity<Product> createProduct(@RequestBody Product product) {
        return ResponseEntity.ok(productRepository.save(product));
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable UUID id, @RequestBody Product productData) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        product.setName(productData.getName());
        product.setDescription(productData.getDescription());
        product.setPrice(productData.getPrice());
        product.setCategory(productData.getCategory());
        product.setImageUrl(productData.getImageUrl());
        product.setIsActive(productData.getIsActive());
        return ResponseEntity.ok(productRepository.save(product));
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        product.setIsActive(false);
        productRepository.save(product);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/inventory/{branchId}")
    public ResponseEntity<List<BranchInventory>> getInventory(@PathVariable UUID branchId) {
        return ResponseEntity.ok(inventoryRepository.findByBranchId(branchId));
    }

    @PutMapping("/inventory/{id}")
    public ResponseEntity<BranchInventory> updateInventory(
            @PathVariable UUID id,
            @RequestBody BranchInventory inventoryData) {
        BranchInventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory record not found"));
        inventory.setStockQuantity(inventoryData.getStockQuantity());
        return ResponseEntity.ok(inventoryRepository.save(inventory));
    }

    @PostMapping("/inventory")
    public ResponseEntity<BranchInventory> createInventory(@RequestBody BranchInventory inventory) {
        return ResponseEntity.ok(inventoryRepository.save(inventory));
    }
}
