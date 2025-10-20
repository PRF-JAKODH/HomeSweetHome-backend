package com.homesweet.homesweetback.domain.auth.dto;

import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.entity.UserRole;
import java.time.LocalDate;

/**
 * 사용자 정보 응답 DTO
 */
public record UserResponse(
    Long id,
    String email,
    String name,
    String profileImageUrl,
    String grade,
    UserRole role,
    String phoneNumber,
    LocalDate birthDate
) {
    public static UserResponse of(User user) {
        return new UserResponse(
            user.getId(),
            user.getEmail(),
            user.getName(),
            user.getProfileImageUrl(),
            user.getGrade(),
            user.getRole(),
            user.getPhoneNumber(),
            user.getBirthDate()
        );
    }
}
