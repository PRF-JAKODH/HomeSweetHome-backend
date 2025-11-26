package com.homesweet.homesweetback.domain.search.community.service.impl;

import com.homesweet.homesweetback.domain.search.community.repository.CommunityPostRepository;
import com.homesweet.homesweetback.domain.search.community.service.CommunitySearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private final CommunityPostRepository communityPostRepository;

    @Override
    public List<String> autocomplete(String keyword) {
        return communityPostRepository.autocomplete(keyword);
    }
}
