package com.homesweet.homesweetback.domain.auth.dto;

import com.homesweet.homesweetback.domain.auth.entity.UserRole;

/**
 * 사용자 역할 변경 요청 DTO
 */
public record UpdateUserRoleRequest(
    UserRole role
) {
}
