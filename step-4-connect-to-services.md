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

Similarly, you can connect to any of your containers using the same approach by using the port-mapping feature of Testcontainers Desktop.

### 
[Next](step-5-write-tests.md)
