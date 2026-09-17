package com.dartcodes.smartorder.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dartcodes.smartorder.dto.*;
import com.dartcodes.smartorder.exception.AllocationFailedException;
import com.dartcodes.smartorder.exception.ResourceNotFoundException;
import com.dartcodes.smartorder.model.*;
import com.dartcodes.smartorder.repository.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final BranchRepository branchRepository;
    private final BranchInventoryRepository inventoryRepository;
    private final UserRepository userRepository;
    private final AllocationService allocationService;
    private final ClassificationService classificationService;
    private final OrderMessageRepository orderMessageRepository;
    private final CartItemRepository cartItemRepository;

    @Transactional
    public OrderResponse createOrder(OrderRequest request, String customerEmail) {
        User customer = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (var itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Product not found: " + itemReq.getProductId()));

            if (!product.getIsActive()) {
                throw new IllegalArgumentException("Product is not available: " + product.getName());
            }

            BigDecimal itemTotal = product.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            totalAmount = totalAmount.add(itemTotal);

            orderItems.add(OrderItem.builder()
                    .productId(product.getId())
                    .quantity(itemReq.getQuantity())
                    .unitPrice(product.getPrice())
                    .build());
        }

        String classifiedCategory = null;
        Double classificationConfidence = null;
        if (request.getCustomerMessage() != null && !request.getCustomerMessage().trim().isEmpty()) {
            var classification = classificationService.classify(request.getCustomerMessage());
            classifiedCategory = classification.category();
            classificationConfidence = classification.confidence();
        }

        AllocationService.AllocationResult allocation;
        try {
            allocation = allocationService.allocate(
                    request.getItems(),
                    request.getCustomerLatitude(),
                    request.getCustomerLongitude()
            );
        } catch (AllocationFailedException ex) {
            Order rejectedOrder = Order.builder()
                    .customerId(customer.getId())
                    .status("REJECTED")
                    .totalAmount(totalAmount)
                    .customerLatitude(request.getCustomerLatitude())
                    .customerLongitude(request.getCustomerLongitude())
                    .customerAddress(request.getCustomerAddress())
                    .customerMessage(request.getCustomerMessage())
                    .classifiedCategory(classifiedCategory)
                    .classificationConfidence(classificationConfidence)
                    .allocationReason(ex.getMessage())
                    .updatedAt(OffsetDateTime.now())
                    .build();
            rejectedOrder = orderRepository.save(rejectedOrder);

            for (var item : orderItems) {
                item.setOrderId(rejectedOrder.getId());
            }
            orderItemRepository.saveAll(orderItems);

            return mapToResponse(rejectedOrder, orderItems, null, customer);
        }

        Order order = Order.builder()
                .customerId(customer.getId())
                .allocatedBranchId(allocation.branchId())
                .status("ALLOCATED")
                .totalAmount(totalAmount)
                .customerLatitude(request.getCustomerLatitude())
                .customerLongitude(request.getCustomerLongitude())
                .customerAddress(request.getCustomerAddress())
                .customerMessage(request.getCustomerMessage())
                .classifiedCategory(classifiedCategory)
                .classificationConfidence(classificationConfidence)
                .allocationReason(allocation.reason())
                .updatedAt(OffsetDateTime.now())
                .build();
        order = orderRepository.save(order);

        for (var item : orderItems) {
            item.setOrderId(order.getId());
        }
        orderItemRepository.saveAll(orderItems);

        if (request.getCustomerMessage() != null && !request.getCustomerMessage().trim().isEmpty()) {
            orderMessageRepository.save(OrderMessage.builder()
                    .orderId(order.getId())
                    .senderId(customer.getId())
                    .senderName(customer.getName())
                    .senderRole(customer.getRole())
                    .message(request.getCustomerMessage().trim())
                    .classifiedCategory(classifiedCategory)
                    .classificationConfidence(classificationConfidence)
                    .createdAt(OffsetDateTime.now())
                    .build());
        }

        for (var itemReq : request.getItems()) {
            BranchInventory inv = inventoryRepository
                    .findByBranchIdAndProductId(allocation.branchId(), itemReq.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Inventory record not found"));
            inv.setStockQuantity(inv.getStockQuantity() - itemReq.getQuantity());
            inventoryRepository.save(inv);
        }

        Branch branch = branchRepository.findById(allocation.branchId())
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
        branch.setCurrentWorkload(branch.getCurrentWorkload() + 1);
        branchRepository.save(branch);

        log.info("Order {} allocated to branch {} ({})", order.getId(), branch.getName(), allocation.reason());

        cartItemRepository.deleteByUserId(customer.getId());

        return mapToResponse(order, orderItems, branch, customer);
    }

    public OrderResponse getOrderById(UUID orderId, String userEmail) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if ("CUSTOMER".equals(user.getRole()) && !order.getCustomerId().equals(user.getId())) {
            throw new IllegalArgumentException("Access denied");
        }

        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        Branch branch = order.getAllocatedBranchId() != null
                ? branchRepository.findById(order.getAllocatedBranchId()).orElse(null)
                : null;
        User customer = userRepository.findById(order.getCustomerId()).orElse(null);

        return mapToResponse(order, items, branch, customer);
    }

    public List<OrderResponse> getOrdersByCustomer(String customerEmail) {
        User customer = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        List<Order> orders = orderRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId());
        return orders.stream().map(order -> {
            List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
            Branch branch = order.getAllocatedBranchId() != null
                    ? branchRepository.findById(order.getAllocatedBranchId()).orElse(null)
                    : null;
            return mapToResponse(order, items, branch, customer);
        }).toList();
    }

    public List<OrderResponse> getAllOrders() {
        List<Order> orders = orderRepository.findAllByOrderByCreatedAtDesc();
        return orders.stream().map(order -> {
            List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
            Branch branch = order.getAllocatedBranchId() != null
                    ? branchRepository.findById(order.getAllocatedBranchId()).orElse(null)
                    : null;
            User customer = userRepository.findById(order.getCustomerId()).orElse(null);
            return mapToResponse(order, items, branch, customer);
        }).toList();
    }

    @Transactional
    public OrderResponse updateOrderStatus(UUID orderId, String newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        validateStatusTransition(order.getStatus(), newStatus);

        order.setStatus(newStatus);
        order.setUpdatedAt(OffsetDateTime.now());

        if ("DELIVERED".equals(newStatus) && order.getAllocatedBranchId() != null) {
            Branch branch = branchRepository.findById(order.getAllocatedBranchId()).orElse(null);
            if (branch != null && branch.getCurrentWorkload() > 0) {
                branch.setCurrentWorkload(branch.getCurrentWorkload() - 1);
                branchRepository.save(branch);
            }
        }

        order = orderRepository.save(order);

        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        Branch branch = order.getAllocatedBranchId() != null
                ? branchRepository.findById(order.getAllocatedBranchId()).orElse(null)
                : null;
        User customer = userRepository.findById(order.getCustomerId()).orElse(null);

        return mapToResponse(order, items, branch, customer);
    }

    @Transactional
    public OrderResponse cancelOrder(UUID orderId, String userEmail) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if ("CUSTOMER".equals(user.getRole()) && !order.getCustomerId().equals(user.getId())) {
            throw new IllegalArgumentException("Access denied");
        }

        if ("DELIVERED".equals(order.getStatus()) || "CANCELLED".equals(order.getStatus())) {
            throw new IllegalArgumentException("Cannot cancel an order that is " + order.getStatus());
        }

        order.setStatus("CANCELLED");
        order.setUpdatedAt(OffsetDateTime.now());

        if (order.getAllocatedBranchId() != null) {
            List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
            for (var item : items) {
                inventoryRepository.findByBranchIdAndProductId(
                        order.getAllocatedBranchId(), item.getProductId()
                ).ifPresent(inv -> {
                    inv.setStockQuantity(inv.getStockQuantity() + item.getQuantity());
                    inventoryRepository.save(inv);
                });
            }

            Branch branch = branchRepository.findById(order.getAllocatedBranchId()).orElse(null);
            if (branch != null && branch.getCurrentWorkload() > 0) {
                branch.setCurrentWorkload(branch.getCurrentWorkload() - 1);
                branchRepository.save(branch);
            }
        }

        order = orderRepository.save(order);

        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        Branch branch = order.getAllocatedBranchId() != null
                ? branchRepository.findById(order.getAllocatedBranchId()).orElse(null)
                : null;
        User customer = userRepository.findById(order.getCustomerId()).orElse(null);

        return mapToResponse(order, items, branch, customer);
    }

    @Transactional
    public OrderResponse confirmDelivery(UUID orderId, String userEmail) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if ("CUSTOMER".equals(user.getRole()) && !order.getCustomerId().equals(user.getId())) {
            throw new IllegalArgumentException("Access denied");
        }

        if (!"READY".equals(order.getStatus())) {
            throw new IllegalArgumentException("Order can only be confirmed as delivered when it is READY");
        }

        return updateOrderStatus(orderId, "DELIVERED");
    }

    private void validateStatusTransition(String currentStatus, String newStatus) {
        boolean valid = switch (currentStatus) {
            case "ALLOCATED" -> "PREPARING".equals(newStatus) || "CANCELLED".equals(newStatus);
            case "PREPARING" -> "READY".equals(newStatus) || "CANCELLED".equals(newStatus);
            case "READY" -> "DELIVERED".equals(newStatus);
            default -> false;
        };

        if (!valid) {
            throw new IllegalArgumentException(
                    String.format("Cannot transition from %s to %s", currentStatus, newStatus));
        }
    }

    private OrderResponse mapToResponse(Order order, List<OrderItem> items, Branch branch, User customer) {
        var itemResponses = items.stream().map(item -> {
            String productName = productRepository.findById(item.getProductId())
                    .map(Product::getName).orElse("Unknown");
            return OrderResponse.OrderItemResponse.builder()
                    .id(item.getId())
                    .productId(item.getProductId())
                    .productName(productName)
                    .quantity(item.getQuantity())
                    .unitPrice(item.getUnitPrice())
                    .build();
        }).toList();

        OrderResponse.BranchInfo branchInfo = null;
        if (branch != null) {
            branchInfo = OrderResponse.BranchInfo.builder()
                    .id(branch.getId())
                    .name(branch.getName())
                    .address(branch.getAddress())
                    .build();
        }

        return OrderResponse.builder()
                .id(order.getId())
                .customerId(order.getCustomerId())
                .customerName(customer != null ? customer.getName() : null)
                .customerEmail(customer != null ? customer.getEmail() : null)
                .customerAddress(order.getCustomerAddress())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .customerLatitude(order.getCustomerLatitude())
                .customerLongitude(order.getCustomerLongitude())
                .customerMessage(order.getCustomerMessage())
                .classifiedCategory(order.getClassifiedCategory())
                .classificationConfidence(order.getClassificationConfidence())
                .allocationReason(order.getAllocationReason())
                .allocatedBranch(branchInfo)
                .items(itemResponses)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    @Transactional
    public OrderMessageResponse addOrderMessage(UUID orderId, String messageText, String userEmail) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if ("CUSTOMER".equals(user.getRole()) && !order.getCustomerId().equals(user.getId())) {
            throw new IllegalArgumentException("Access denied");
        }

        boolean isCustomer = "CUSTOMER".equalsIgnoreCase(user.getRole());
        ClassificationService.ClassificationResult classification = isCustomer ? classificationService.classify(messageText) : null;

        OrderMessage msg = OrderMessage.builder()
                .orderId(order.getId())
                .senderId(user.getId())
                .senderName(user.getName())
                .senderRole(user.getRole())
                .message(messageText)
                .classifiedCategory(classification != null ? classification.category() : null)
                .classificationConfidence(classification != null ? classification.confidence() : null)
                .createdAt(OffsetDateTime.now())
                .build();

        msg = orderMessageRepository.save(msg);

        if (isCustomer) {
            order.setCustomerMessage(messageText);
            if (classification != null && classification.category() != null) {
                order.setClassifiedCategory(classification.category());
                order.setClassificationConfidence(classification.confidence());
            }
            orderRepository.save(order);
        }

        return mapToMessageResponse(msg);
    }

    public List<OrderMessageResponse> getOrderMessages(UUID orderId, String userEmail) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if ("CUSTOMER".equals(user.getRole()) && !order.getCustomerId().equals(user.getId())) {
            throw new IllegalArgumentException("Access denied");
        }

        return orderMessageRepository.findByOrderIdOrderByCreatedAtAsc(orderId)
                .stream()
                .map(this::mapToMessageResponse)
                .toList();
    }

    private OrderMessageResponse mapToMessageResponse(OrderMessage msg) {
        return OrderMessageResponse.builder()
                .id(msg.getId())
                .orderId(msg.getOrderId())
                .senderId(msg.getSenderId())
                .senderName(msg.getSenderName())
                .senderRole(msg.getSenderRole())
                .message(msg.getMessage())
                .classifiedCategory(msg.getClassifiedCategory())
                .classificationConfidence(msg.getClassificationConfidence())
                .createdAt(msg.getCreatedAt())
                .build();
    }
}
