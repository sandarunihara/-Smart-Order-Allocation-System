package com.dartcodes.smartorder.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.dartcodes.smartorder.model.OrderMessage;

@Repository
public interface OrderMessageRepository extends JpaRepository<OrderMessage, UUID> {
    List<OrderMessage> findByOrderIdOrderByCreatedAtAsc(UUID orderId);
}
