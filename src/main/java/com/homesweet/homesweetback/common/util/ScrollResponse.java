package com.homesweet.homesweetback.common.util;

import java.util.List;

/**
 * 제품 무한 스크롤 응답 DTO
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 23.
 */
public record ScrollResponse<T>(
        List<T> contents,
        Long nextCursorId,
        boolean hasNext
) {
    public static <T> ScrollResponse<T> of(List<T> contents, Long nextCursorId, boolean hasNext) {
        return new ScrollResponse<>(contents, nextCursorId, hasNext);
    }
}