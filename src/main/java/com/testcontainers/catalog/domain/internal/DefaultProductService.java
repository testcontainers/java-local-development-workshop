package com.testcontainers.catalog.domain.internal;

import com.testcontainers.catalog.clients.inventory.InventoryServiceClient;
import com.testcontainers.catalog.domain.FileStorageService;
import com.testcontainers.catalog.domain.ProductService;
import com.testcontainers.catalog.domain.models.CreateProductRequest;
import com.testcontainers.catalog.domain.models.Product;
import com.testcontainers.catalog.domain.models.ProductImageUploadedEvent;
import java.io.InputStream;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
class DefaultProductService implements ProductService {
    private static final Logger log = LoggerFactory.getLogger(DefaultProductService.class);

    private final ProductRepository productRepository;
    private final InventoryServiceClient inventoryServiceClient;
    private final FileStorageService fileStorageService;
    private final ProductEventPublisher productEventPublisher;

    public DefaultProductService(
            ProductRepository productRepository,
            InventoryServiceClient inventoryServiceClient,
            FileStorageService fileStorageService,
            ProductEventPublisher productEventPublisher) {
        this.productRepository = productRepository;
        this.inventoryServiceClient = inventoryServiceClient;
        this.fileStorageService = fileStorageService;
        this.productEventPublisher = productEventPublisher;
    }

    public void createProduct(CreateProductRequest request) {
        ProductEntity entity = new ProductEntity();
        entity.setCode(request.code());
        entity.setName(request.name());
        entity.setDescription(request.description());
        entity.setPrice(request.price());

        productRepository.save(entity);
    }

    public Optional<Product> getProductByCode(String code) {
        Optional<ProductEntity> productEntity = productRepository.findByCode(code);
        if (productEntity.isEmpty()) {
            return Optional.empty();
        }
        return productEntity.map(this::toProduct);
    }

    public void uploadProductImage(String code, String imageName, InputStream inputStream) {
        fileStorageService.upload(imageName, inputStream);
        productEventPublisher.publish(new ProductImageUploadedEvent(code, imageName));
        log.info("Published event to update product image for code: {}", code);
    }

    public void updateProductImage(String code, String image) {

//        productRepository.updateProductImage(code, image);
    }

    private boolean isProductAvailable(String code) {
        try {
            return inventoryServiceClient.getInventory(code).quantity() > 0;
        } catch (Exception e) {
            log.error("Error while calling inventory service", e);
            // business decision is to show as available if inventory service is down
            return true;
        }
    }

    private Product toProduct(ProductEntity entity) {
        return new Product(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getDescription(),
                StringUtils.hasText(entity.getImage()) ? fileStorageService.getPreSignedURL(entity.getImage()) : null,
                entity.getPrice(),
                isProductAvailable(entity.getCode()));
    }
}
