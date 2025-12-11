<<<<<<<< HEAD:src/main/java/com/homesweet/homesweetback/domain/product/recent/service/RecentSearchService.java
package com.homesweet.homesweetback.domain.product.recent.service;
========
package com.homesweet.homesweetback.domain.product.product.command.service;
>>>>>>>> 9de1dca (feat: CQRS에 맞는 폴더 구조 설정):src/main/java/com/homesweet/homesweetback/domain/product/product/command/service/RecentSearchService.java

import java.util.List;

/**
 * 최근 검색 조회 서비스
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 13.
 */
public interface RecentSearchService {

    void save(Long userId, String keyword);

    List<String> getRecent(Long userId);

    void deleteKeyword(Long userId, String keyword);

    void clearAll(Long userId);

}
