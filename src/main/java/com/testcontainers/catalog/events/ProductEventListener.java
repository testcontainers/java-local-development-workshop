package com.testcontainers.catalog.events;

import com.testcontainers.catalog.domain.ProductService;
import com.testcontainers.catalog.domain.models.ProductImageUploadedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
class ProductEventListener {
    private static final Logger log = LoggerFactory.getLogger(ProductEventListener.class);

    private final ProductService productService;

    ProductEventListener(ProductService productService) {
        this.productService = productService;
    }

    @KafkaListener(topics = "${application.product-image-updates-topic}", groupId = "catalog-service")
    public void handle(ProductImageUploadedEvent event) {
        log.info("Received a ProductImageUploaded with code:{}", event.code());
        productService.updateProductImage(event.code(), event.image());
    }
}
