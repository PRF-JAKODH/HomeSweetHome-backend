package com.homesweet.homesweetback.common.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.homesweet.homesweetback.domain.search.chat.sync.repository.ChatRoomDocumentRepository;
import com.homesweet.homesweetback.domain.search.community.sync.repository.CommunityPostDocumentRepository;
import com.homesweet.homesweetback.domain.search.product.sync.repository.ProductDocumentRepository;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;

/**
 * 엘라스틱 테스트 컨테이너
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 28.
 */
@Testcontainers
public class ElasticSearchTestContainer {

    private static final String ELASTICSEARCH_IMAGE = "docker.elastic.co/elasticsearch/elasticsearch:8.6.0";

    @Container
    private static final ElasticsearchContainer container = new ElasticsearchContainer(ELASTICSEARCH_IMAGE)
            .withEnv("discovery.type", "single-node")
            .withEnv("xpack.security.enabled", "false")
            .withEnv("xpack.security.http.ssl.enabled", "false")
            .withCommand("sh", "-c", "elasticsearch-plugin install analysis-nori && elasticsearch");

    static {
        container.start();
    }

    @DynamicPropertySource
    static void setElasticsearchProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.elasticsearch.uris", container::getHttpHostAddress);
    }

    @Bean
    public RestClient restClient() {
        return RestClient.builder(HttpHost.create(container.getHttpHostAddress())).build();
    }

    @Bean
    public ElasticsearchClient elasticsearchClient(RestClient restClient) {
        return new ElasticsearchClient(new RestClientTransport(restClient, new JacksonJsonpMapper()));
    }
}