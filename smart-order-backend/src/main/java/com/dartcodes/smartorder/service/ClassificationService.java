package com.dartcodes.smartorder.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ClassificationService {

    @Value("${ml.service.url:http://localhost:8000}")
    private String mlServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public ClassificationResult classify(String message) {
        if (message == null || message.trim().isEmpty()) {
            return new ClassificationResult(null, null, false);
        }

        try {
            Map<String, String> body = Map.of("message", message);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    mlServiceUrl + "/classify", body, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> result = response.getBody();
                String category = (String) result.get("category");
                Number confidence = (Number) result.get("confidence");
                Boolean isConfident = (Boolean) result.get("is_confident");

                return new ClassificationResult(
                        category,
                        confidence != null ? confidence.doubleValue() : null,
                        isConfident != null && isConfident
                );
            }
        } catch (Exception ex) {
            log.warn("ML classification service unavailable: {}", ex.getMessage());
        }

        return new ClassificationResult(null, null, false);
    }

    public record ClassificationResult(String category, Double confidence, boolean isConfident) {}
}
