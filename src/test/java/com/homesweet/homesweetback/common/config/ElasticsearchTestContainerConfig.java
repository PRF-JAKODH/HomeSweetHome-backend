package com.homesweet.homesweetback.common.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;

/**
 * 엘라스틱 테스트 컨테이너
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 28.
 */
@TestConfiguration
public class ElasticsearchTestContainerConfig {

    @Container
    static final ElasticsearchContainer container =
            new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:8.13.2")
                    .withEnv("discovery.type", "single-node")
                    .withEnv("xpack.security.enabled", "false");

    static {
        container.start();
    }
}
