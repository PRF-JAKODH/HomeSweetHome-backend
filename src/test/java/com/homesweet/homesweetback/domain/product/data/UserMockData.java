package com.homesweet.homesweetback.domain.product.data;

import com.homesweet.homesweetback.domain.auth.entity.User;

/**
 * 사용자 관련 Mock 객체 생성
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 7.
 */
public class UserMockData {

    // 사용자 생성
    public static User createMockUser(Long id, String name) {
        return User.builder()
                .id(id)
                .name(name)
                .build();
    }
}
