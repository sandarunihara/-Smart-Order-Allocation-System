package com.dartcodes.smartorder.controller;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.dartcodes.smartorder.dto.*;
import com.dartcodes.smartorder.service.OrderService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody OrderRequest request,
            Principal principal) {
        return ResponseEntity.ok(orderService.createOrder(request, principal.getName()));
    }

    @GetMapping("/my-orders")
    public ResponseEntity<List<OrderResponse>> getMyOrders(Principal principal) {
        return ResponseEntity.ok(orderService.getOrdersByCustomer(principal.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(
            @PathVariable UUID id,
            Principal principal) {
        return ResponseEntity.ok(orderService.getOrderById(id, principal.getName()));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable UUID id,
            Principal principal) {
        return ResponseEntity.ok(orderService.cancelOrder(id, principal.getName()));
    }

    @PutMapping("/{id}/deliver")
    public ResponseEntity<OrderResponse> confirmDelivery(
            @PathVariable UUID id,
            Principal principal) {
        return ResponseEntity.ok(orderService.confirmDelivery(id, principal.getName()));
    }

    @GetMapping("/{id}/messages")
    public ResponseEntity<List<OrderMessageResponse>> getOrderMessages(
            @PathVariable UUID id,
            Principal principal) {
        return ResponseEntity.ok(orderService.getOrderMessages(id, principal.getName()));
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<OrderMessageResponse> addOrderMessage(
            @PathVariable UUID id,
            @Valid @RequestBody OrderMessageRequest request,
            Principal principal) {
        return ResponseEntity.ok(orderService.addOrderMessage(id, request.getMessage(), principal.getName()));
    }
}
