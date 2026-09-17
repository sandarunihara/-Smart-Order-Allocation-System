package com.dartcodes.smartorder.dto;

import java.math.BigDecimal;
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
public class DashboardStats {
    private long totalOrders;
    private long pendingOrders;
    private long allocatedOrders;
    private long preparingOrders;
    private long deliveredOrders;
    private long cancelledOrders;
    private long rejectedOrders;
    private BigDecimal totalRevenue;
    private List<BranchStats> branchStats;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BranchStats {
        private UUID branchId;
        private String branchName;
        private long activeOrders;
        private int currentWorkload;
        private int maxWorkload;
        private boolean isActive;
    }
}
