package com.abarigena.taskflow.config;

import org.apache.http.HttpHost;
import org.opensearch.client.RestClient;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.rest_client.RestClientTransport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Конфигурация OpenSearch для TaskFlow
 * Простая конфигурация без аутентификации для dev окружения
 */
@Configuration
public class OpenSearchConfig {

    @Value("${opensearch.host:localhost}")
    private String opensearchHost;

    @Value("${opensearch.port:9200}")
    private int opensearchPort;

    @Bean
    public OpenSearchClient openSearchClient() {
        RestClient restClient = RestClient.builder(
                new HttpHost(opensearchHost, opensearchPort, "http")
        ).build();
        
        RestClientTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        
        return new OpenSearchClient(transport);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
} 