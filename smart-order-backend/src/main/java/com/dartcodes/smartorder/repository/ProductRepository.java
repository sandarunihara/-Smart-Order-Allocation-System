package com.dartcodes.smartorder.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.dartcodes.smartorder.model.Product;

@Repository 
public interface ProductRepository extends JpaRepository<Product, UUID> {
    List<Product> findByIsActiveTrue();
    List<Product> findByCategory(String category);
    List<Product> findByNameContainingIgnoreCase(String name);
}
