package com.dartcodes.smartorder.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.dartcodes.smartorder.model.Branch;

@Repository 
public interface BranchRepository extends JpaRepository<Branch, UUID> {
    List<Branch> findByIsActiveTrue();
}
