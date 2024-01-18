package com.testcontainers.catalog.domain.models;

import java.math.BigDecimal;

public record Product(
        Long id, String code, String name, String description, String imageUrl, Double price, boolean available) {}
