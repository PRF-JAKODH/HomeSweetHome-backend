package com.homesweet.homesweetback.domain.search.community.service.impl;

import com.homesweet.homesweetback.common.util.scroll.CursorUtil;
import com.homesweet.homesweetback.common.util.scroll.SearchScrollResponse;
import com.homesweet.homesweetback.domain.product.recent.service.RecentSearchService;
import com.homesweet.homesweetback.domain.search.community.controller.response.CommunityPostSearchResponse;
import com.homesweet.homesweetback.domain.search.community.controller.response.CommunitySortType;
import com.homesweet.homesweetback.domain.search.community.repository.CommunityPostSearchRepository;
import com.homesweet.homesweetback.domain.search.community.repository.document.CommunityPostDocument;
import com.homesweet.homesweetback.domain.search.community.service.CommunitySearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 커뮤니티 서비스 구현체
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 26.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommunitySearchServiceImpl implements CommunitySearchService {

    private final CommunityPostSearchRepository communityPostRepository;
    private final RecentSearchService recentSearchService;
    private final CursorUtil cursorUtil;

    @Override
    public List<String> autocomplete(String keyword) {
        return communityPostRepository.autocomplete(keyword);
    }

    @Override
    public SearchScrollResponse<CommunityPostSearchResponse> search(Long userId, String cursor, String keyword, int limit, CommunitySortType sortType) {

        if (userId != null && keyword != null && !keyword.isBlank()) {
            recentSearchService.save(userId, keyword);
        }
        SearchHits<CommunityPostDocument> hits =
                communityPostRepository.search(keyword, cursor, limit, sortType);

        List<CommunityPostDocument> docs = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .toList();

        boolean hasNext = docs.size() > limit;
        List<CommunityPostDocument> result = hasNext ? docs.subList(0, limit) : docs;

        CommunityPostDocument lastDoc = hasNext ? result.getLast() : null;
        Float lastScore = hasNext ? hits.getSearchHits().get(limit - 1).getScore() : null;

        List<Object> sortValues = null;
        if (lastDoc != null) {

            switch (sortType) {

                case RECOMMENDED -> {
                    sortValues = List.of(
                            lastScore != null ? lastScore : 0.0f,
                            lastDoc.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                            lastDoc.getPostId()
                    );
                }

                case LATEST -> {
                    sortValues = List.of(
                            lastDoc.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                            lastDoc.getPostId()
                    );
                }

                case VIEW_COUNT -> {
                    sortValues = List.of(
                            lastDoc.getViewCount(),
                            lastDoc.getPostId()
                    );
                }

                case LIKE_COUNT -> {
                    sortValues = List.of(
                            lastDoc.getLikeCount(),
                            lastDoc.getPostId()
                    );
                }

            }
        }

        String nextCursor = cursorUtil.encode(sortValues);

        List<CommunityPostSearchResponse> responses = result.stream()
                .map(CommunityPostSearchResponse::from)
                .toList();

        return SearchScrollResponse.of(responses, nextCursor, hasNext);
    }
}