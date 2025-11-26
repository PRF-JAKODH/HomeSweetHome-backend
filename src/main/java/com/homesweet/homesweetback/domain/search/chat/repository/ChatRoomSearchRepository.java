package com.homesweet.homesweetback.domain.search.chat.repository;

import java.util.List;

/**
 * 채팅방 검색 레포지토리
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 26.
 */
public interface ChatRoomSearchRepository {

    List<String> autocomplete(String keyword);

}
