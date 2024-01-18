package com.testcontainers.catalog.domain.internal;

import com.azure.spring.data.cosmos.core.mapping.Container;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

//@Entity
//@Table(name = "products")
@Container(containerName = "person", ru = "400")
public class ProductEntity {
    private Long id;

    private String code;

    private String name;

    private String description;

    private String image;

    private Double price;

    public ProductEntity() {}

    public ProductEntity(Long id, String code, String name, String description, String image, Double price) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.description = description;
        this.image = image;
        this.price = price;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }
}
