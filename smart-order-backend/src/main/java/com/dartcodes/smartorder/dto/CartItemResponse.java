package com.dartcodes.smartorder.dto;

import com.dartcodes.smartorder.model.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemResponse {
    private UUID id;
    private Product product;
    private Integer quantity;
}
