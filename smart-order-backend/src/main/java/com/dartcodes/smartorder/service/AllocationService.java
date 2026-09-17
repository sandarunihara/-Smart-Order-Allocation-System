package com.dartcodes.smartorder.service;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.dartcodes.smartorder.dto.OrderRequest;
import com.dartcodes.smartorder.exception.AllocationFailedException;
import com.dartcodes.smartorder.model.Branch;
import com.dartcodes.smartorder.model.BranchInventory;
import com.dartcodes.smartorder.repository.BranchInventoryRepository;
import com.dartcodes.smartorder.repository.BranchRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Core allocation engine using weighted multi-factor scoring.
 *
 * Score = (0.40 × StockScore) + (0.35 × WorkloadScore) + (0.25 × ProximityScore)
 *
 * - StockScore: min(available / requested) across all items, normalized 0–1
 * - WorkloadScore: 1 - (currentWorkload / maxWorkload), normalized 0–1
 * - ProximityScore: 1 - (distance / maxDistance), normalized 0–1
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AllocationService {

    // Rebalanced allocation weights: Proximity (55%) > Workload (30%) > Stock Health (15%)
    private static final double WEIGHT_PROXIMITY = 0.55;
    private static final double WEIGHT_WORKLOAD = 0.30;
    private static final double WEIGHT_STOCK = 0.15;

    private final BranchRepository branchRepository;
    private final BranchInventoryRepository inventoryRepository;

    /**
     * Allocates the best branch for an order.
     * Returns an AllocationResult with branch ID, score, and human-readable reason.
     */
    public AllocationResult allocate(List<OrderRequest.OrderItemRequest> items,
                                     Double customerLat, Double customerLng) {
        // 1. Get all active branches
        List<Branch> activeBranches = branchRepository.findByIsActiveTrue();
        if (activeBranches.isEmpty()) {
            throw new AllocationFailedException("No active branches available");
        }

        // 2. Build item demand map: productId -> quantity
        Map<UUID, Integer> demandMap = new HashMap<>();
        for (var item : items) {
            demandMap.merge(item.getProductId(), item.getQuantity(), Integer::sum);
        }

        // 3. Filter branches that have ALL products in sufficient stock
        List<BranchCandidate> candidates = new ArrayList<>();
        for (Branch branch : activeBranches) {
            List<BranchInventory> inventory = inventoryRepository.findByBranchId(branch.getId());
            Map<UUID, Integer> stockMap = inventory.stream()
                    .collect(Collectors.toMap(BranchInventory::getProductId, BranchInventory::getStockQuantity));

            boolean hasAllItems = true;
            double minStockRatio = Double.MAX_VALUE;

            for (var entry : demandMap.entrySet()) {
                UUID productId = entry.getKey();
                int requested = entry.getValue();
                int available = stockMap.getOrDefault(productId, 0);

                if (available < requested) {
                    hasAllItems = false;
                    break;
                }

                double ratio = (double) available / requested;
                minStockRatio = Math.min(minStockRatio, ratio);
            }

            if (hasAllItems) {
                candidates.add(new BranchCandidate(branch, minStockRatio));
            }
        }

        if (candidates.isEmpty()) {
            throw new AllocationFailedException(
                "No branch has sufficient stock for all items in this order"
            );
        }

        // 4. Single candidate → direct allocation
        if (candidates.size() == 1) {
            Branch b = candidates.get(0).branch;
            return new AllocationResult(b.getId(), b.getName(), 1.0,
                    "Only branch with sufficient stock: " + b.getName());
        }

        // 5. Calculate distances (if customer location is provided)
        boolean hasLocation = customerLat != null && customerLng != null;
        double maxDistance = 0;
        Map<UUID, Double> distances = new HashMap<>();
        if (hasLocation) {
            for (var c : candidates) {
                double dist = haversine(customerLat, customerLng,
                        c.branch.getLatitude(), c.branch.getLongitude());
                distances.put(c.branch.getId(), dist);
                maxDistance = Math.max(maxDistance, dist);
            }
        }

        // Normalize stock ratios
        double maxStockRatio = candidates.stream()
                .mapToDouble(c -> c.minStockRatio).max().orElse(1.0);

        // 6. Score each candidate
        BranchCandidate bestCandidate = null;
        double bestScore = -1;
        StringBuilder reason = new StringBuilder();

        for (var c : candidates) {
            // Branches with healthy stock (>= 2x requested) get full 1.0 stock score
            double stockScore = Math.min(c.minStockRatio / 2.0, 1.0);

            double workloadScore = 1.0 - ((double) c.branch.getCurrentWorkload()
                    / Math.max(c.branch.getMaxWorkload(), 1));
            workloadScore = Math.max(0, workloadScore);

            double proximityScore = (hasLocation && maxDistance > 0)
                    ? 1.0 - (distances.get(c.branch.getId()) / maxDistance)
                    : 1.0;

            double totalScore = (WEIGHT_STOCK * stockScore)
                    + (WEIGHT_WORKLOAD * workloadScore)
                    + (WEIGHT_PROXIMITY * proximityScore);

            log.info("Branch {} [{}]: stock={}, workload={}, proximity={}, total={}",
                    c.branch.getName(), c.branch.getId(),
                    stockScore, workloadScore, proximityScore, totalScore);

            if (totalScore > bestScore ||
                    (totalScore == bestScore && bestCandidate != null &&
                            c.branch.getCurrentWorkload() < bestCandidate.branch.getCurrentWorkload())) {
                bestScore = totalScore;
                bestCandidate = c;
            }
        }

        if (bestCandidate == null) {
            throw new AllocationFailedException("Allocation scoring failed unexpectedly");
        }

        Branch best = bestCandidate.branch;
        Double dist = distances.get(best.getId());
        String distanceStr = dist != null ? String.format("%.1f km", dist) : "N/A (District/Address based)";
        reason.append(String.format(
                "Selected %s (score: %.2f). Stock ratio: %.1fx, Workload: %d/%d, Distance: %s. " +
                        "Evaluated %d eligible branches out of %d active.",
                best.getName(), bestScore, bestCandidate.minStockRatio,
                best.getCurrentWorkload(), best.getMaxWorkload(),
                distanceStr, candidates.size(), activeBranches.size()
        ));

        return new AllocationResult(best.getId(), best.getName(), bestScore, reason.toString());
    }

    /**
     * Haversine formula to calculate distance between two lat/lng points in km.
     */
    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0; // Earth radius in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    // Inner classes
    private static class BranchCandidate {
        final Branch branch;
        final double minStockRatio;

        BranchCandidate(Branch branch, double minStockRatio) {
            this.branch = branch;
            this.minStockRatio = minStockRatio;
        }
    }

    public record AllocationResult(UUID branchId, String branchName, double score, String reason) {}
}
