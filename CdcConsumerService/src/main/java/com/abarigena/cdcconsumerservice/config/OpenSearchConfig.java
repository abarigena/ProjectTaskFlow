package com.abarigena.cdcconsumerservice.config;

import org.apache.http.HttpHost;
import org.opensearch.client.RestClient;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.rest_client.RestClientTransport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация OpenSearch для CdcConsumerService
 */
@Configuration
public class OpenSearchConfig {

    @Value("${opensearch.host:localhost}")
    private String opensearchHost;

    @Value("${opensearch.port:9200}")
    private int opensearchPort;

    @Bean
    public OpenSearchClient openSearchClient() {
        // Создание REST клиента для соединения с OpenSearch
        RestClient restClient = RestClient.builder(
            new HttpHost(opensearchHost, opensearchPort, "http")
        ).build();

        // Создание транспорта с Jackson JSON mapper
        RestClientTransport transport = new RestClientTransport(
            restClient, new JacksonJsonpMapper()
        );

        // Создание OpenSearch клиента
        return new OpenSearchClient(transport);
    }
} 