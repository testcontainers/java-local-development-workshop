# Step 6: Let's write tests
So far, we focused on being able to run the application locally without having to install or run any dependent services manually.
But there is nothing more painful than working on a codebase without a comprehensive test suite.

Let's fix that!!

## Common Test SetUp
For all the integration tests in our application, we need to start PostgreSQL, Kafka, LocalStack and WireMock containers.
So, let's create a `BaseIntegrationTest` class under `src/test/java` with the common setup as follows:

```java
package com.testcontainers.catalog;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import com.testcontainers.catalog.ContainersConfig;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
    webEnvironment = RANDOM_PORT,
    properties = {
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "spring.cloud.aws.region.static=us-east-1"
})
@Import(ContainersConfig.class)
public abstract class BaseIntegrationTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUpBase() {
        RestAssured.port = port;
    }
}
```

* We have reused the `ContainersConfig` class that we created in the previous steps.
* We have configured the `spring.kafka.consumer.auto-offset-reset` property to `earliest` to make sure that we can read all the messages from the beginning of the topic.
* We have configured the `RestAssured.port` to the dynamic port of the application that is started by Spring Boot.

## First Test - Verify Application Context Starts Successfully
Let's create/update the test class `ApplicationTests` under `src/test/java` with the following test:

```java
package com.testcontainers.catalog;

import org.junit.jupiter.api.Test;

class ApplicationTests extends BaseIntegrationTest {

    @Test
    void contextLoads() {}
}
```

## Lets add tests for ProductController API endpoints
Before writing the API tests, let's create `src/test/resources/test-data.sql` to insert some test data into the database as follows:

```sql
DELETE FROM products;

insert into products(code, name, description, image, price) values
('P101','Product P101','Product P101 description', null, 34.0),
('P102','Product P102','Product P102 description', null, 25.0),
('P103','Product P103','Product P103 description', null, 15.0)
;
```

Create `ProductControllerTest` and add a test to successfully create a new product as follows:

```java
package com.testcontainers.catalog.api;

import com.testcontainers.catalog.BaseIntegrationTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.endsWith;

@Sql("/test-data.sql")
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
```

Next, let's add a test for product image upload API endpoint.

```java
package com.testcontainers.catalog.api;

import com.testcontainers.catalog.BaseIntegrationTest;
import com.testcontainers.catalog.domain.ProductService;
import com.testcontainers.catalog.domain.models.Product;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.jdbc.Sql;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.endsWith;

@Sql("/test-data.sql")
class ProductControllerTest extends BaseIntegrationTest {
    @Autowired
    ProductService productService;
    
    @Test
    void shouldUploadProductImageSuccessfully() throws IOException {
        String code = "P101";
        File file = new ClassPathResource("P101.jpg").getFile();

        Optional<Product> product = productService.getProductByCode(code);
        assertThat(product).isPresent();
        assertThat(product.get().imageUrl()).isNull();

        given().multiPart("file", file, "multipart/form-data")
                .contentType(ContentType.MULTIPART)
                .when()
                .post("/api/products/{code}/image", code)
                .then()
                .statusCode(200)
                .body("status", endsWith("success"))
                .body("filename", endsWith("P101.jpg"));

        await().pollInterval(Duration.ofSeconds(3)).atMost(10, SECONDS).untilAsserted(() -> {
            Optional<Product> optionalProduct = productService.getProductByCode(code);
            assertThat(optionalProduct).isPresent();
            assertThat(optionalProduct.get().imageUrl()).isNotEmpty();
        });
    }
}
```
