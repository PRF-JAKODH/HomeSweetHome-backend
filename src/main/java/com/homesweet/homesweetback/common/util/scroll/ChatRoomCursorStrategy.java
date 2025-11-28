package com.homesweet.homesweetback.common.util.scroll;

import com.homesweet.homesweetback.domain.search.chat.controller.response.ChatRoomSortType;

import java.util.List;

/**
 * 채팅방 커서 전략 패턴
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 26.
 */
public class ChatRoomCursorStrategy implements CursorStrategy {

    private final ChatRoomSortType sortType;

    public ChatRoomCursorStrategy(ChatRoomSortType sortType) {
        this.sortType = sortType;
    }

    @Override
    public List<Object> extractSortValues(List<?> rawList) {
        return switch (sortType) {
            case RECOMMENDED -> rawList.size() >= 3
                    ? List.of(rawList.get(0), rawList.get(1), rawList.get(2)) : null;
            case LATEST -> rawList.size() >= 2
                    ? List.of(rawList.get(0), rawList.get(1)) : null;
        };
    }
}
