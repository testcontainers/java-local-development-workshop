package com.testcontainers.catalog;

import com.testcontainers.catalog.BaseIntegrationTest;
import com.testcontainers.catalog.domain.ProductService;
import com.testcontainers.catalog.domain.models.Product;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.endsWith;

class ProductControllerTest extends BaseIntegrationTest {

    @Test
    void createProductSuccessfully() {
        String code = UUID.randomUUID().toString();
        given().contentType(ContentType.JSON)
                .body(
                        """
                    {
                        "code": "%s",
                        "name": "Product %s",
                        "description": "Product %s description",
                        "price": 10.0
                    }
                    """
                                .formatted(code, code, code))
                .when()
                .post("/api/products")
                .then()
                .statusCode(201)
                .header("Location", endsWith("/api/products/%s".formatted(code)));
    }

}