package com.dartcodes.smartorder.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity 
@Table(name = "orders")
@Data 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "allocated_branch_id")
    private UUID allocatedBranchId;

    @Column(length = 50)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "customer_latitude")
    private Double customerLatitude;

    @Column(name = "customer_longitude")
    private Double customerLongitude;

    @Column(name = "customer_address", length = 255)
    private String customerAddress;

    @Column(name = "customer_message", columnDefinition = "TEXT")
    private String customerMessage;

    @Column(name = "classified_category", length = 100)
    private String classifiedCategory;

    @Column(name = "classification_confidence")
    private Double classificationConfidence;

    @Column(name = "allocation_reason", columnDefinition = "TEXT")
    private String allocationReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", insertable = false, updatable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "allocated_branch_id", insertable = false, updatable = false)
    private Branch allocatedBranch;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderItem> items;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}