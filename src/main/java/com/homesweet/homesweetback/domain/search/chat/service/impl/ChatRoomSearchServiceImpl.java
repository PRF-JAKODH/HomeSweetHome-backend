package com.homesweet.homesweetback.domain.search.chat.service.impl;

import com.homesweet.homesweetback.domain.search.chat.repository.ChatRoomSearchRepository;
import com.homesweet.homesweetback.domain.search.chat.service.ChatRoomSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 단체 채팅방 검색 서비스 구현체
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 26.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomSearchServiceImpl implements ChatRoomSearchService {

    private final ChatRoomSearchRepository chatRoomSearchRepository;

    @Override
    public List<String> autocomplete(String keyword) {
        return chatRoomSearchRepository.autocomplete(keyword);
    }
}
