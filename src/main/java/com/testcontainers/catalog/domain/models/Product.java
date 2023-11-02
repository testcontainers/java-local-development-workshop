package com.testcontainers.catalog.domain.models;

import java.math.BigDecimal;

public record Product(
        Long id, String code, String name, String description, String imageUrl, BigDecimal price, boolean available) {}
