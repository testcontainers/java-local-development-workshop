package com.testcontainers.catalog.domain;

import com.testcontainers.catalog.domain.models.CreateProductRequest;
import com.testcontainers.catalog.domain.models.Product;
import java.io.InputStream;
import java.util.Optional;

public interface ProductService {

    void createProduct(CreateProductRequest request);

    Optional<Product> getProductByCode(String code);

    void uploadProductImage(String code, String imageName, InputStream inputStream);

    void updateProductImage(String code, String image);
}
