package com.abarigena.cdcconsumerservice.config;

import org.apache.http.HttpHost;
import org.opensearch.client.RestClient;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.rest_client.RestClientTransport;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenSearchConfig {

    @Bean
    public OpenSearchClient openSearchClient() {
        // Создание REST клиента для соединения с OpenSearch
        RestClient restClient = RestClient.builder(
            new HttpHost("localhost", 9200, "http")
        ).build();

        // Создание транспорта с Jackson JSON mapper
        OpenSearchTransport transport = new RestClientTransport(
            restClient, new JacksonJsonpMapper()
        );

        // Создание OpenSearch клиента
        return new OpenSearchClient(transport);
    }
} 