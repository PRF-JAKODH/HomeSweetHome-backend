package com.homesweet.homesweetback.common.util.scroll;

import java.util.List;

/**
 * 커서 전략 인터페이스
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 26.
 */
public interface CursorStrategy {
    List<Object> extractSortValues(List<?> rawList);
}