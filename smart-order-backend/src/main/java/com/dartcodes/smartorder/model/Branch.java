package com.dartcodes.smartorder.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity 
@Table(name = "branches")
@Data 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder 
public class Branch {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(name = "current_workload")
    @Builder.Default
    private Integer currentWorkload = 0;

    @Column(name = "max_workload")
    @Builder.Default
    private Integer maxWorkload = 50;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
}
