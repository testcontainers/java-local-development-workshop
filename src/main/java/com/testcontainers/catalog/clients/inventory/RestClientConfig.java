package com.testcontainers.catalog.clients.inventory;

import com.testcontainers.catalog.ApplicationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
class RestClientConfig {

    @Bean
    InventoryServiceClient inventoryServiceProxy(ApplicationProperties properties) {
        RestClient restClient = RestClient.create(properties.inventoryServiceUrl());
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
                .build();
        return factory.createClient(InventoryServiceClient.class);
    }
}
