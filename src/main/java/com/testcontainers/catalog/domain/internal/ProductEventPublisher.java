package com.testcontainers.catalog.domain.internal;

import com.testcontainers.catalog.ApplicationProperties;
import com.testcontainers.catalog.domain.models.ProductImageUploadedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
class ProductEventPublisher {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ApplicationProperties properties;

    public ProductEventPublisher(KafkaTemplate<String, Object> kafkaTemplate, ApplicationProperties properties) {
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
    }

    public void publish(ProductImageUploadedEvent event) {
        kafkaTemplate.send(properties.productImageUpdatesTopic(), event.code(), event);
    }
}
