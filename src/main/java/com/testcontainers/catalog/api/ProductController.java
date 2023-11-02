package com.testcontainers.catalog.api;

import com.testcontainers.catalog.domain.ProductNotFoundException;
import com.testcontainers.catalog.domain.ProductService;
import com.testcontainers.catalog.domain.models.CreateProductRequest;
import com.testcontainers.catalog.domain.models.Product;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/products")
class ProductController {
    private final ProductService productService;

    ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    ResponseEntity<Void> createProduct(@Validated @RequestBody CreateProductRequest request) {
        productService.createProduct(request);
        URI uri = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/products/{code}")
                .buildAndExpand(request.code())
                .toUri();
        return ResponseEntity.created(uri).build();
    }

    @GetMapping("/{code}")
    ResponseEntity<Product> getProductByCode(@PathVariable String code) {
        var product = productService.getProductByCode(code).orElseThrow(() -> ProductNotFoundException.withCode(code));
        return ResponseEntity.ok(product);
    }

    @PostMapping("/{code}/image")
    ResponseEntity<Map<String, String>> uploadProductImage(
            @PathVariable String code, @RequestParam("file") MultipartFile file) throws IOException {
        var filename = file.getOriginalFilename();
        var extn = filename.substring(filename.lastIndexOf("."));
        var imageName = code + extn;
        productService.uploadProductImage(code, imageName, file.getInputStream());
        Map<String, String> response = Map.of("status", "success", "filename", imageName);
        return ResponseEntity.ok(response);
    }
}
