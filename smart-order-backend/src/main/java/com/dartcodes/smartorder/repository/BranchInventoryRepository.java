package com.dartcodes.smartorder.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.dartcodes.smartorder.model.BranchInventory;

@Repository 
public interface BranchInventoryRepository extends JpaRepository<BranchInventory, UUID> {
    List<BranchInventory> findByBranchId(UUID branchId);
    Optional<BranchInventory> findByBranchIdAndProductId(UUID branchId, UUID productId);
    List<BranchInventory> findByProductId(UUID productId);
}
