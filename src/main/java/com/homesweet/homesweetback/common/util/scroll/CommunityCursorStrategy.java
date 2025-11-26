package com.homesweet.homesweetback.common.util.scroll;

import com.homesweet.homesweetback.domain.search.community.controller.response.CommunitySortType;

import java.util.List;

/**
 * 커뮤니티 게시글 커서 전략 패턴
 *
 * @author …
 */
public class CommunityCursorStrategy implements CursorStrategy {

    private final CommunitySortType sortType;

    public CommunityCursorStrategy(CommunitySortType sortType) {
        this.sortType = sortType;
    }

    @Override
    public List<Object> extractSortValues(List<?> rawList) {

        return switch (sortType) {

            case RECOMMENDED ->
                    rawList.size() >= 3
                            ? List.of(rawList.get(0), rawList.get(1), rawList.get(2))
                            : null;

            case LATEST ->
                    rawList.size() >= 2
                            ? List.of(rawList.get(0), rawList.get(1))
                            : null;

            case VIEW_COUNT ->
                    rawList.size() >= 2
                            ? List.of(rawList.get(0), rawList.get(1))
                            : null;

            case LIKE_COUNT ->
                    rawList.size() >= 2
                            ? List.of(rawList.get(0), rawList.get(1))
                            : null;
        };
    }
}