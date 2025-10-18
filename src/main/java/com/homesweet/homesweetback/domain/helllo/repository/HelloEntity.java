package com.homesweet.homesweetback.domain.helllo.repository;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Hello 엔티티
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 18.
 */
@Getter
@Builder
public class HelloEntity {
    private Long id;
    private String name;
}
