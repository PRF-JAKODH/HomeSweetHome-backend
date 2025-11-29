package com.homesweet.homesweetback.domain.search.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 엘라스틱 서치 설정 파일
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 26.
 */
@Configuration
@ConditionalOnProperty(name = "search.elasticsearch.enabled", havingValue = "true")
public class ElasticsearchConfig {

    @Value("${spring.elasticsearch.uris}")
    private String elasticsearchUrl;

    @Bean
    public RestClient restClient() {

        return RestClient.builder(HttpHost.create(elasticsearchUrl))

                // ES HTTP Connection Pool 튜닝
                .setHttpClientConfigCallback(httpClientBuilder ->
                        httpClientBuilder
                                // 전체 커넥션 수
                                .setMaxConnTotal(300)
                                // 노드당 커넥션 수
                                .setMaxConnPerRoute(300)
                                // Keep-Alive (커넥션 재사용)
                                .setKeepAliveStrategy((response, context) -> 30_000) // 30초
                )

                // Request Timeout, Connect Timeout 설정
                .setRequestConfigCallback(requestConfig ->
                        requestConfig
                                .setConnectTimeout(1500)
                                .setSocketTimeout(30_000)
                                .setConnectionRequestTimeout(500)
                )

                .build();
    }

    @Bean
    public ElasticsearchTransport elasticsearchTransport(RestClient restClient) {
        return new RestClientTransport(restClient, new JacksonJsonpMapper());
    }

    @Bean(name = "esRestClientBuilder")
    public ElasticsearchClient elasticsearchClient(ElasticsearchTransport transport) {
        return new ElasticsearchClient(transport);
    }
}
