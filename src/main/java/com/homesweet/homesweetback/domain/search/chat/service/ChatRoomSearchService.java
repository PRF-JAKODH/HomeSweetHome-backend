package com.homesweet.homesweetback.domain.search.chat.service;

import java.util.List;

/**
 * 단체 채팅방 검색 서비스
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 26.
 */
public interface ChatRoomSearchService {

    List<String> autocomplete(String keyword);

}
