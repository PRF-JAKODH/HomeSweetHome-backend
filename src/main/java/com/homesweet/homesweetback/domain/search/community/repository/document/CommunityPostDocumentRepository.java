package com.homesweet.homesweetback.domain.search.community.repository.document;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * 게시글 도큐먼트 레포지토리
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 26.
 */
public interface CommunityPostDocumentRepository extends ElasticsearchRepository<CommunityPostDocument, Long> {
}
