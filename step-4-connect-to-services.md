# Step 4: Connect to services

In the previous step, we get our application running locally and invoked our API endpoints.

What if you want to check the data in the database or the messages in Kafka?

Testcontainers by default start the containers and map the exposed ports on a random available port on the host machine.
Each time you restart the application, the mapped ports will be different.
This is good for testing, but for local development and debugging, it would be convenient to be able to connect on fixed ports.

This is where **Testcontainers Desktop** helps you.

## Testcontainers Desktop
Testcontainers Desktop application provides several features that helps you with local development and debugging.
To learn more about Testcontainers Desktop, check out the [Simple local development with Testcontainers Desktop](https://testcontainers.com/guides/simple-local-development-with-testcontainers-desktop/) guide.

The Testcontainers Desktop app makes it easy to use fixed ports for your containers,
so that you can always connect to those services using the same fixed port.

## Connect to PostgreSQL database
Click on **Testcontainers Desktop → select Services → Open config location...**.

In the opened directory there would be a `postgres.toml.example` file. 
Make a copy of it and rename it to `postgres.toml` file and update it with the following content:

```toml
ports = [
    {local-port = 5432, container-port = 5432},
]
selector.image-names = ["postgres"]
```

We are mapping the PostgreSQL container's port 5432 onto the host's port 5432.
Now you should be able to connect to the PostgreSQL database using any SQL client 
with the following connection properties:

```shell
psql -h localhost -p 5432 -U test -d test
```

## Connect to Kafka
While you can use any of your existing tools to connect to these containerized services,
you can also run a helper container that will allow you to connect to the services.

For example, you can start a **Kafka Control Center** container and connect to the Kafka instance via web browser.

Let's update `KafkaContainer` configuration in `ContainersConfig` as follows:

```java
package com.testcontainers.catalog;

...
...
import static org.testcontainers.utility.DockerImageName.parse;

import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.lifecycle.Startables;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;

@TestConfiguration(proxyBeanMethods = false)
public class ContainersConfig {
    ...
    ...

    @Bean
    @ServiceConnection
    KafkaContainer kafkaContainer() {
        Network network = getNetwork();
        KafkaContainer kafka = new KafkaContainer(parse("confluentinc/cp-kafka:7.5.0"))
                .withEnv("KAFKA_CONFLUENT_SCHEMA_REGISTRY_URL", "http://schemaregistry:8085")
                .withNetworkAliases("kafka")
                .withNetwork(network);

        GenericContainer<?> schemaRegistry = new GenericContainer<>("confluentinc/cp-schema-registry:7.5.0")
                .withExposedPorts(8085)
                .withNetworkAliases("schemaregistry").withNetwork(network)
                .withEnv("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS", "PLAINTEXT://kafka:9092")
                .withEnv("SCHEMA_REGISTRY_LISTENERS", "http://0.0.0.0:8085")
                .withEnv("SCHEMA_REGISTRY_HOST_NAME", "schemaregistry")
                .withEnv("SCHEMA_REGISTRY_KAFKASTORE_SECURITY_PROTOCOL", "PLAINTEXT")
                .waitingFor(Wait.forHttp("/subjects"))
                .withStartupTimeout(Duration.of(120, ChronoUnit.SECONDS))
                .dependsOn(kafka);

        GenericContainer<?> controlCenter =
                new GenericContainer<>("confluentinc/cp-enterprise-control-center:7.5.0")
                        .withExposedPorts(9021,9022)
                        .withNetwork(network)
                        .withEnv("CONTROL_CENTER_BOOTSTRAP_SERVERS", "BROKER://kafka:9092")
                        .withEnv("CONTROL_CENTER_REPLICATION_FACTOR", "1")
                        .withEnv("CONTROL_CENTER_INTERNAL_TOPICS_PARTITIONS", "1")
                        .withEnv("CONTROL_CENTER_SCHEMA_REGISTRY_SR1_URL", "http://schemaregistry:8085")
                        .withEnv("CONTROL_CENTER_SCHEMA_REGISTRY_URL", "http://schemaregistry:8085")
                        .dependsOn(kafka, schemaRegistry)
                        .waitingFor(Wait.forHttp("/clusters").forPort(9021).allowInsecure())
                        .withStartupTimeout(Duration.of(120, ChronoUnit.SECONDS))
                        .withLabel("com.testcontainers.desktop.service", "cp-control-center");
        Startables.deepStart(kafka, schemaRegistry, controlCenter).join();
        return kafka;
    }

    private Network getNetwork() {
        String networkId = "kafka";
        Network defaultNetwork = new Network() {
            @Override
            public String getId() {
                return networkId;
            }

            @Override
            public void close() {}

            @Override
            public Statement apply(Statement base, Description description) {
                return null;
            }
        };

        List<com.github.dockerjava.api.model.Network>
                networks = DockerClientFactory.instance()
                .client().listNetworksCmd().withNameFilter(networkId).exec();
        if (networks.isEmpty()) {
            Network.builder().createNetworkCmdModifier(cmd -> cmd.withName(networkId)).build().getId();
        }
        return defaultNetwork;
    }
}
```

We are using the `confluentinc/cp-enterprise-control-center:7.5.0` image to start the Kafka Control Center container.
The Control Center is a web-based tool for managing and monitoring Apache Kafka clusters.
Let's map its port 9021 to the host's port 19021.

Create `kafka-control-center.toml` file in the Testcontainers Desktop config directory with the following content:

```toml
ports = [
  {local-port = 19021, container-port = 9021},
]
selector.image-names = ["confluentinc/cp-enterprise-control-center"]
```

Now, if you restart the application, you should be able to connect to the Kafka Control Center at http://localhost:19021/.

* Invoke the Product Image Upload API
* Go to the Kafka Control Center at http://localhost:19021/
* Click on the **Cluster** tab
* Click on the **Topics** tab
* Click on the topic `product-image-updates`
* Click on the **Messages** tab

You should be able to see the messages sent to Kafka.

### 
[Next](step-5-use-reusable-containers.md)
