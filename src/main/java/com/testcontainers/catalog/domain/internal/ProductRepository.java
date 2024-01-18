package com.testcontainers.catalog.domain.internal;

import java.util.Optional;

import com.azure.spring.data.cosmos.repository.CosmosRepository;

public interface ProductRepository extends CosmosRepository<ProductEntity, String> {
}
