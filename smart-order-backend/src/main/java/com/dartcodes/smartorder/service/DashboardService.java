package com.dartcodes.smartorder.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dartcodes.smartorder.dto.DashboardStats;
import com.dartcodes.smartorder.model.Branch;
import com.dartcodes.smartorder.model.Order;
import com.dartcodes.smartorder.repository.BranchRepository;
import com.dartcodes.smartorder.repository.OrderRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final OrderRepository orderRepository;
    private final BranchRepository branchRepository;

    @Transactional(readOnly = true)
    public DashboardStats getDashboardStats() {
        List<Order> allOrders = orderRepository.findAll();

        BigDecimal totalRevenue = allOrders.stream()
                .filter(o -> o.getTotalAmount() != null && !"CANCELLED".equals(o.getStatus()) && !"REJECTED".equals(o.getStatus()))
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Branch> branches = branchRepository.findAll();
        List<DashboardStats.BranchStats> branchStats = branches.stream().map(branch -> {
            long activeOrders = orderRepository.countActiveOrdersByBranch(branch.getId());
            return DashboardStats.BranchStats.builder()
                    .branchId(branch.getId())
                    .branchName(branch.getName() != null ? branch.getName() : "Unnamed Branch")
                    .activeOrders(activeOrders)
                    .currentWorkload(branch.getCurrentWorkload() != null ? branch.getCurrentWorkload() : 0)
                    .maxWorkload(branch.getMaxWorkload() != null && branch.getMaxWorkload() > 0 ? branch.getMaxWorkload() : 50)
                    .isActive(Boolean.TRUE.equals(branch.getIsActive()))
                    .build();
        }).toList();

        return DashboardStats.builder()
                .totalOrders(allOrders.size())
                .pendingOrders(orderRepository.countByStatus("PENDING"))
                .allocatedOrders(orderRepository.countByStatus("ALLOCATED"))
                .preparingOrders(orderRepository.countByStatus("PREPARING"))
                .deliveredOrders(orderRepository.countByStatus("DELIVERED"))
                .cancelledOrders(orderRepository.countByStatus("CANCELLED"))
                .rejectedOrders(orderRepository.countByStatus("REJECTED"))
                .totalRevenue(totalRevenue)
                .branchStats(branchStats)
                .build();
    }
}
