package com.testcontainers.catalog.domain.models;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record CreateProductRequest(
        @NotEmpty String code, @NotEmpty String name, String description, @NotNull @Positive BigDecimal price) {}
