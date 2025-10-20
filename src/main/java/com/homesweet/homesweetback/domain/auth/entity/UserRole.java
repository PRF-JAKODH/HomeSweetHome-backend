package com.homesweet.homesweetback.domain.auth.entity;

/**
 * 사용자 역할을 정의하는 Enum
 * Spring Security와 호환되도록 ROLE_ 접두사를 포함
 */
public enum UserRole {
    USER("ROLE_USER"),
    SELLER("ROLE_SELLER");

    private final String authority;

    UserRole(String authority) {
        this.authority = authority;
    }

    /**
     * Spring Security의 authority 문자열을 반환
     * @return ROLE_ 접두사가 포함된 authority 문자열
     */
    public String getAuthority() {
        return authority;
    }

    /**
     * authority 문자열로부터 UserRole을 찾는 메서드
     * @param authority ROLE_ 접두사가 포함된 authority 문자열
     * @return 해당하는 UserRole, 없으면 null
     */
    public static UserRole fromAuthority(String authority) {
        for (UserRole role : values()) {
            if (role.authority.equals(authority)) {
                return role;
            }
        }
        return null;
    }
}
