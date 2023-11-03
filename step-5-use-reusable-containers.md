# Step 5: Use reusable containers

During the development, you will keep changing the code and verify the behavior either by running the tests 
or running the application locally. Recreating the containers everytime you restart the application 
might slow down your quick feedback cycle. 

One technique that you can apply to speed up testing and local development is using the **reusable containers** feature.

Since you are using the Testcontainers Desktop, the `testcontainers.reuse.enable` flag is set automatically 
for your dev environment. 
You can enable or disable it by clicking on **Enable reusable containers** option under **Preferences**.

Once the `reuse` feature is enabled, you need to configure which containers should be reused using the Testcontainers API. 
With Testcontainers for Java API, you can achieve this using `.withReuse(true)` as follows:

```java
@TestConfiguration(proxyBeanMethods = false)
public class ContainersConfig {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>(parse("postgres:16-alpine")).withReuse(true);
    }
    
    //Similarly add .withReuse(true) for other containers
    
}
```

* When you first start the application, the containers will be created. 
* When you stop the application, the containers will continue to run. 
* When you restart the application again, the containers will be reused.

If you no longer want to keep the containers running, then you can remove them by clicking on **Testcontainers Desktop → Terminate containers**.

### 
[Next](step-6-write-tests.md)
