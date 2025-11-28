package com.homesweet.homesweetback.common.util.scroll;

import java.util.List;

/**
 * 제품 무한 스크롤 응답 DTO
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 23.
 */
public record SearchScrollResponse<T>(
        List<T> contents,
        String nextCursor,
        boolean hasNext
) {
    public static <T> SearchScrollResponse<T> of(List<T> contents, String nextCursor, boolean hasNext) {
        return new SearchScrollResponse<>(contents, nextCursor, hasNext);
    }
}