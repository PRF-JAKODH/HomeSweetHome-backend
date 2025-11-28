package com.homesweet.homesweetback.common.config;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * 통합 테스트 지원 클래스
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 28.
 */
@ActiveProfiles("test")
@Import(ElasticsearchTestContainerConfig.class)
@SpringBootTest
public abstract class IntegrationTestSupport {
}
