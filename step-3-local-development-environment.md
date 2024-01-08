# Step 3: Local development environment with Testcontainers
Our application uses PostgreSQL, Kafka, and LocalStack. 

Currently, if you run the `Application.java` from your IDE, you will see the following error:

```shell
***************************
APPLICATION FAILED TO START
***************************

Description:

Failed to configure a DataSource: 'url' attribute is not specified and no embedded datasource could be configured.

Reason: Failed to determine a suitable driver class

Action:

Consider the following:
	If you want an embedded database (H2, HSQL or Derby), please put it on the classpath.
	If you have database settings to be loaded from a particular profile you may need to activate it (no profiles are currently active).

Process finished with exit code 0
```

To run the application locally, we need to have these services up and running.

Instead of installing these services on our local machine, or using Docker to run these services manually,
we will use [Spring Boot support for Testcontainers at Development Time](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#features.testing.testcontainers.at-development-time) to provision these services automatically.

> **NOTE**
>
> Before Spring Boot 3.1.0, Testcontainers libraries are mainly used for testing.
Spring Boot 3.1.0 introduced out-of-the-box support for Testcontainers which not only simplified testing, 
but we can use Testcontainers for local development as well. 
> 
> To learn more, please read [Spring Boot Application Testing and Development with Testcontainers](https://www.atomicjar.com/2023/05/spring-boot-3-1-0-testcontainers-for-testing-and-local-development/) 

First, make sure you have the following Testcontainers dependencies in your `pom.xml`:

```xml
<dependencies>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-testcontainers</artifactId>
    <scope>test</scope>
  </dependency>
  <dependency>
      <groupId>org.testcontainers</groupId>
      <artifactId>postgresql</artifactId>
      <scope>test</scope>
  </dependency>
  <dependency>
      <groupId>org.testcontainers</groupId>
      <artifactId>kafka</artifactId>
      <scope>test</scope>
  </dependency>
  <dependency>
      <groupId>org.testcontainers</groupId>
      <artifactId>localstack</artifactId>
      <scope>test</scope>
  </dependency>
</dependencies>
```

We will also use **RestAssured** for API testing and **Awaitility** for testing asynchronous processes.

So, add the following dependencies as well:

```xml
<dependency>
    <groupId>io.rest-assured</groupId>
    <artifactId>rest-assured</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.awaitility</groupId>
    <artifactId>awaitility</artifactId>
    <scope>test</scope>
</dependency>
```

## Create ContainersConfig class under src/test/java
Let's create `ContainersConfig` class under `src/test/java` to configure the required containers.

```java
package com.testcontainers.catalog;

import static org.testcontainers.utility.DockerImageName.parse;

import com.testcontainers.catalog.domain.FileStorageService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;

@TestConfiguration(proxyBeanMethods = false)
public class ContainersConfig {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>(parse("postgres:16-alpine"));
    }

    @Bean
    @ServiceConnection
    KafkaContainer kafkaContainer() {
        return new KafkaContainer(parse("confluentinc/cp-kafka:7.5.0"));
    }

    @Bean("localstackContainer")
    LocalStackContainer localstackContainer(DynamicPropertyRegistry registry) {
        LocalStackContainer localStack = new LocalStackContainer(parse("localstack/localstack:2.3"));
        registry.add("spring.cloud.aws.credentials.access-key", localStack::getAccessKey);
        registry.add("spring.cloud.aws.credentials.secret-key", localStack::getSecretKey);
        registry.add("spring.cloud.aws.region.static", localStack::getRegion);
        registry.add("spring.cloud.aws.endpoint", localStack::getEndpoint);
        return localStack;
    }

    @Bean
    @DependsOn("localstackContainer")
    ApplicationRunner awsInitializer(ApplicationProperties properties, FileStorageService fileStorageService) {
        return args -> fileStorageService.createBucket(properties.productImagesBucketName());
    }
}
```

Let's understand what this configuration class does:
* `@TestConfiguration` annotation indicates that this configuration class defines the beans that can be used for Spring Boot tests.
* Spring Boot provides `ServiceConnection` support for `JdbcConnectionDetails` and `KafkaConnectionDetails` out-of-the-box.
  So, we configured `PostgreSQLContainer` and `KafkaContainer` as beans with `@ServiceConnection` annotation.
  This configuration will automatically start these containers and register the **DataSource** and **Kafka** connection properties automatically.
* Spring Cloud AWS doesn't provide ServiceConnection support out-of-the-box [yet](https://github.com/awspring/spring-cloud-aws/issues/793).
  But there is support for [Contributing Dynamic Properties at Development Time](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#features.testing.testcontainers.at-development-time.dynamic-properties).
  So, we configured `LocalStackContainer` as a bean and registered the Spring Cloud AWS configuration properties using `DynamicPropertyRegistry`.
* We also configured an `ApplicationRunner` bean to create the AWS resources like S3 bucket upon application startup.
  
## Create TestApplication class under src/test/java
Next, let's create a `TestApplication` class under `src/test/java` to start the application with the Testcontainers configuration.

```java
package com.testcontainers.catalog;

import org.springframework.boot.SpringApplication;

public class TestApplication {

    public static void main(String[] args) {
        SpringApplication
                //note that we are starting our actual Application from within our TestApplication
                .from(Application::main) 
                .with(ContainersConfig.class)
                .run(args);
    }
}
```

Run the `TestApplication` from our IDE and verify that the application starts successfully.

Now, you can invoke the APIs using CURL or Postman or any of your favourite HTTP Client tools.

### Create a product
```shell
curl -v -X "POST" 'http://localhost:8080/api/products' \
--header 'Content-Type: application/json' \
--data '{
"code": "P201",
"name": "Product P201",
"description": "Product P201 description",
"price": 24.0
}'
```

You should get a response similar to the following:

```shell
< HTTP/1.1 201
< Location: http://localhost:8080/api/products/P201
< Content-Length: 0
```

### Upload Product Image
```shell
curl -X "POST" 'http://localhost:8080/api/products/P101/image' \
--form 'file=@"/Users/siva/work/product-p101.jpg"'
```

You should see a response similar to the following:

```shell
{"filename":"P101.jpg","status":"success"}
```

### Get a product by code

```shell
curl -X "GET" 'http://localhost:8080/api/products/P101'
```

You should be able to see the response similar to the following:

```json
{
  "id":1,
  "code":"P101",
  "name":"Product P101",
  "description":"Product P101 description",
  "imageUrl":"http://127.0.0.1:60739/product-images/P101.jpg?X-Amz-Algorithm=AWS4-HMAC-SHA256&...",
  "price":34.0,
  "available":true
}
```

If you check the application logs, you should see the following error in logs:

```shell
com.testcontainers.catalog.domain.internal.DefaultProductService - Error while calling inventory service
org.springframework.web.client.ResourceAccessException: I/O error on GET request for "http://localhost:8081/api/inventory/P101": null
	at org.springframework.web.client.DefaultRestClient$DefaultRequestBodyUriSpec.createResourceAccessException(DefaultRestClient.java:489)
	at org.springframework.web.client.DefaultRestClient$DefaultRequestBodyUriSpec.exchangeInternal(DefaultRestClient.java:414)
	at org.springframework.web.client.DefaultRestClient$DefaultRequestBodyUriSpec.retrieve(DefaultRestClient.java:380)
    ...
    ...
	at jdk.proxy4/jdk.proxy4.$Proxy179.getInventory(Unknown Source)
	at com.testcontainers.catalog.domain.internal.DefaultProductService.isProductAvailable(DefaultProductService.java:68)
	at com.testcontainers.catalog.domain.internal.DefaultProductService.toProduct(DefaultProductService.java:84)
	at java.base/java.util.Optional.map(Optional.java:260)
```

When we invoke the `GET /api/products/{code}` API endpoint, 
the application tried to call the inventory service to get the inventory details.
As the inventory service is not running, we get the above error.

Let's use WireMock to mock the inventory service APIs for our local development and testing.

## Configure WireMock
Add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>org.wiremock.integrations.testcontainers</groupId>
    <artifactId>wiremock-testcontainers-module</artifactId>
    <version>1.0-alpha-13</version>
    <scope>test</scope>
</dependency>
```

Create `src/test/resources/mocks-config.json` to define Mock API behaviour.

```json
{
  "mappings": [
    {
      "request": {
        "method": "GET",
        "urlPattern": "/api/inventory/P101"
      },
      "response": {
        "status": 200,
        "headers": {
          "Content-Type": "application/json"
        },
        "jsonBody": {
          "code": "P101",
          "quantity": 25
        }
      }
    },
    {
      "request": {
        "method": "GET",
        "urlPattern": "/api/inventory/P102"
      },
      "response": {
        "status": 500,
        "headers": {
          "Content-Type": "application/json"
        }
      }
    },
    {
      "request": {
        "method": "GET",
        "urlPattern": "/api/inventory/P103"
      },
      "response": {
        "status": 200,
        "headers": {
          "Content-Type": "application/json"
        },
        "jsonBody": {
          "code": "P103",
          "quantity": 0
        }
      }
    }
  ]
}
```

Next, update the `ContainersConfig` class to configure the `WireMockContainer` as follows:

```java
package com.testcontainers.catalog;

import org.wiremock.integrations.testcontainers.WireMockContainer;

@TestConfiguration(proxyBeanMethods = false)
public class ContainersConfig {

    // [...]

    @Bean
    WireMockContainer wiremockServer(DynamicPropertyRegistry registry) {
        WireMockContainer wiremockServer = new WireMockContainer("wiremock/wiremock:3.2.0-alpine")
                .withMappingFromResource("mocks-config.json");
        registry.add("application.inventory-service-url", wiremockServer::getBaseUrl);
        return wiremockServer;
    }
}
```

Once the WireMock server is started, we are registering the WireMock server URL as `application.inventory-service-url`.
So, when we make a call to `inventory-service` from our application, it will call the WireMock server instead.

Now restart the `TestApplication` and invoke the `GET /api/products/P101` API again.

```shell
curl -X "GET" 'http://localhost:8080/api/products/P101'
```

You should see the response similar to the following:

```json
{
  "id":1,
  "code":"P101",
  "name":"Product P101",
  "description":"Product P101 description",
  "imageUrl":null,
  "price":34.0,
  "available":true
}
```

And there should be no error in the console logs.

Try `curl -X "GET" 'http://localhost:8080/api/products/P103'`. 
You should get the following response with `"available":false` because we mocked inventory-service such that the quantity for P103 to be 0.

```json
{
  "id":3,
  "code":"P103",
  "name":"Product P103",
  "description":"Product P103 description",
  "imageUrl":null,
  "price":15.0,
  "available":false
}
```

Now we have a working local development environment with PostgreSQL, Kafka, LocalStack, and WireMock.

### 
[Next](step-4-connect-to-services.md)
