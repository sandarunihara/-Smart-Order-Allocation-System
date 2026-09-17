package com.dartcodes.smartorder.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderMessageResponse {
    private UUID id;
    private UUID orderId;
    private UUID senderId;
    private String senderName;
    private String senderRole;
    private String message;
    private String classifiedCategory;
    private Double classificationConfidence;
    private OffsetDateTime createdAt;
}
