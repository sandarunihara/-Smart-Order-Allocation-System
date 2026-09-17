package com.dartcodes.smartorder.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OrderMessageRequest {
    @NotBlank(message = "Message content is required")
    private String message;
}
