package com.homesweet.homesweetback.domain.search.community.sync.service;

import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.community.dto.exception.CommunityException;
import com.homesweet.homesweetback.domain.community.entity.CommunityPostEntity;
import com.homesweet.homesweetback.domain.community.repository.CommunityPostRepository;
import com.homesweet.homesweetback.domain.search.community.repository.document.CommunityPostDocument;
import com.homesweet.homesweetback.domain.search.community.sync.repository.CommunityPostDocumentRepository;
import com.homesweet.homesweetback.domain.search.community.sync.mapping.CommunityDocumentMapping;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 게시글 엘라스틱 동기화 서비스
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 27.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "search.elasticsearch.enabled", havingValue = "true")
public class CommunitySyncService {

    private final CommunityPostRepository communityPostRepository;
    private final CommunityPostDocumentRepository communityPostDocumentRepository;
    private final CommunityDocumentMapping communityDocumentMapping;

    @Transactional(readOnly = true)
    public void syncToElasticsearch(Long postId) {
        try {
            CommunityPostEntity post = communityPostRepository.findById(postId)
                    .orElseThrow(() -> new CommunityException(ErrorCode.COMMUNITY_POST_NOT_FOUND));

            CommunityPostDocument document = communityDocumentMapping.convert(post);
            communityPostDocumentRepository.save(document);
            log.info("Elasticsearch 커뮤니티 게시글 동기화 성공 -> postId={}", postId);

        } catch (Exception e) {
            log.error("Failed to sync community post {} to Elasticsearch", postId, e);
        }
    }

    @Transactional
    public void deleteFromElasticsearch(Long postId) {
        try {
            communityPostDocumentRepository.deleteById(postId);
            log.info("Elasticsearch 게시글 삭제 성공 -> postId={}", postId);
        } catch (Exception e) {
            log.error("Failed to delete community post {} from Elasticsearch", postId, e);
        }
    }
}
