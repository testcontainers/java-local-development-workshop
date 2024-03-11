# Step 5: Let's write tests
So far, we focused on being able to run the application locally without having to install or run any dependent services manually.
But there is nothing more painful than working on a codebase without a comprehensive test suite.

Let's fix that!!

## Common Test SetUp
For all the integration tests in our application, we need to start PostgreSQL, Kafka, LocalStack and Microcks containers.
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
import org.testcontainers.Testcontainers;

@SpringBootTest(
    webEnvironment = RANDOM_PORT,
    properties = {
        "spring.kafka.consumer.auto-offset-reset=earliest"
})
@Import(ContainersConfig.class)
public abstract class BaseIntegrationTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUpBase() {
        RestAssured.port = port;
        Testcontainers.exposeHostPorts(port);
    }
}
```

* We have reused the `ContainersConfig` class that we created in the previous steps to define all the required containers.
* We have configured the `spring.kafka.consumer.auto-offset-reset` property to `earliest` to make sure that we read all the messages from the beginning of the topic.
* We have configured the `RestAssured.port` to the dynamic port of the application that is started by Spring Boot.

## First Test - Verify Application Context Starts Successfully
Let's create the test class `ApplicationTests` under `src/test/java` with the following test:

```java
package com.testcontainers.catalog;

import org.junit.jupiter.api.Test;

class ApplicationTests extends BaseIntegrationTest {

    @Test
    void contextLoads() {}
}
```

If you run this test, it should pass and that means we have successfully configured the application to start with all the required containers.

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

Copy any sample image with name `P101.jpg` into `src/main/resources`.

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

This test checks the following:
* Before uploading the image, the product image URL is null for the product with code P101.
* Invoke the Product Image Upload API endpoint with the sample image file.
* Assert that the response status is 200 and the response body contains the image file name.
* Assert that the product image URL is updated in the database after the image upload.

Next, let's add a test for getting the product information by code.

```java
@Sql("/test-data.sql")
class ProductControllerTest extends BaseIntegrationTest {
    @Autowired
    ProductService productService;

    @Test
    void getProductByCodeSuccessfully() {
        String code = "P101";

        Product product = given().contentType(ContentType.JSON)
                .when()
                .get("/api/products/{code}", code)
                .then()
                .statusCode(200)
                .extract()
                .as(Product.class);

        assertThat(product.code()).isEqualTo(code);
        assertThat(product.name()).isEqualTo("Product %s".formatted(code));
        assertThat(product.description()).isEqualTo("Product %s description".formatted(code));
        assertThat(product.price().compareTo(new BigDecimal("34.0"))).isEqualTo(0);
        assertThat(product.available()).isTrue();
    }
}
```

Checking product information like this is easy but become really cumbersome when the number of properties is growing 
or when the `Product` class is shared among many different operations of your API. You have to check the properties
presence but also their type and this can result in sprawling code!

If you're using an "API design-first approach", the conformance of your data structure can be automatically checked by
Microcks for you! Check the `src/main/resources/catalog-openapi.yaml` file that describes our Catalog API.

Now let's create a test that uses Microcks to automatically check that our `ProductController` is conformance to this definition:

```java
import io.github.microcks.testcontainers.MicrocksContainer;
import io.github.microcks.testcontainers.model.TestRequest;
import io.github.microcks.testcontainers.model.TestResult;
import io.github.microcks.testcontainers.model.TestRunnerType;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.jdbc.Sql;

@Sql("/test-data.sql")
class ProductControllerTest extends BaseIntegrationTest {
    @Autowired
    MicrocksContainer microcks;

    @Test
    void checkOpenAPIConformance() throws Exception {
        microcks.importAsMainArtifact(new ClassPathResource("catalog-openapi.yaml").getFile());

        TestRequest testRequest = new TestRequest.Builder()
                .serviceId("Catalog Service:1.0")
                .runnerType(TestRunnerType.OPEN_API_SCHEMA.name())
                .testEndpoint("http://host.testcontainers.internal:" + RestAssured.port)
                .build();

        TestResult testResult = microcks.testEndpoint(testRequest);

        assertThat(testResult.isSuccess()).isTrue();
   }
}
```

Let's understand what's going on behind the scenes:
* We complete the Microcks container with our additional `catalog-openapi.yaml` artifact file (this could have also
been done within the `ContainersConfig` class at bean initialisation).
* We prepare a `TestRequest` object that allows to specify the scope of the conformance test. Here we want to check the
conformance of `Catalog Service` with version `1.0` that are the identifier found in `catalog-openapi.yaml`.
* We ask Microcks to validate the `OpenAPI Schema` conformance by specifying a `runnerType`.
* We ask Microcks to validate the localhost endpoint on the dynamic port provided by the Spring Test 
(we use the `host.testcontainers.internal` alias for that).

Finally, we're retrieving a `TestResult` from Microcks containers, and we can assert stuffs on this result, checking it's a success.

During the test, Microcks has reused all the examples found in the `catalog-openapi.yaml` file to issue requests to
our running application. It also checked that all the received responses conform to the OpenAPI definition elements:
return codes, headers, content-type and JSON schema structure.

If you want to get more details on the test done by Microcks, you can add those lines just before the `assertThat()`:

```java
        // You may inspect complete response object with following:
        ObjectMapper mapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(testResult));
```

## Assignment
* Write tests for create product API fails if the payload is invalid.
* Write tests for create product API fails if the product code already exists.
* Write tests for get product by code API fails if the product code does not exist.
* Write tests for get product by code API that returns `"available": false` when Microcks server return quantity=0.
* Write tests for get product by code API that returns `"available": true` from Microcks server throws Exception.
