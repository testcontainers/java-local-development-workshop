package com.testcontainers.catalog;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

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
    }

}