package com.dartcodes.smartorder.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private UUID id;
    private UUID customerId;
    private String customerName;
    private String customerEmail;
    private String customerAddress;
    private String status;
    private BigDecimal totalAmount;
    private Double customerLatitude;
    private Double customerLongitude;
    private String customerMessage;
    private String classifiedCategory;
    private Double classificationConfidence;
    private String allocationReason;
    private BranchInfo allocatedBranch;
    private List<OrderItemResponse> items;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BranchInfo {
        private UUID id;
        private String name;
        private String address;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemResponse {
        private UUID id;
        private UUID productId;
        private String productName;
        private Integer quantity;
        private BigDecimal unitPrice;
    }
}
