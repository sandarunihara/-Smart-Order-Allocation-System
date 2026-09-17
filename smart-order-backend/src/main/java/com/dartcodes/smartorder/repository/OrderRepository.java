package com.dartcodes.smartorder.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.dartcodes.smartorder.model.Order;

@Repository 
public interface OrderRepository extends JpaRepository<Order, UUID> {
    List<Order> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);
    List<Order> findByAllocatedBranchId(UUID allocatedBranchId);
    List<Order> findByStatus(String status);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status")
    long countByStatus(String status);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.allocatedBranchId = :branchId AND o.status NOT IN ('DELIVERED', 'CANCELLED', 'REJECTED')")
    long countActiveOrdersByBranch(UUID branchId);

    List<Order> findAllByOrderByCreatedAtDesc();
}
