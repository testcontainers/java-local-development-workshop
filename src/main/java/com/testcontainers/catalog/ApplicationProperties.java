package com.testcontainers.catalog;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "application")
@Validated
public record ApplicationProperties(
        @NotEmpty String productImagesBucketName,
        @NotEmpty String productImageUpdatesTopic,
        @NotEmpty String inventoryServiceUrl) {}
