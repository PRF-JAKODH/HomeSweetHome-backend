package com.homesweet.homesweetback.domain.auth.dto;

import com.homesweet.homesweetback.domain.auth.entity.User;
import java.time.LocalDate;

public record UpdateUserRequest(
    String name,
    String profileImageUrl,
    String phoneNumber,
    LocalDate birthDate
) {
    public static UpdateUserRequest of(User user) {
        return new UpdateUserRequest(
            user.getName(),
            user.getProfileImageUrl(),
            user.getPhoneNumber(),
            user.getBirthDate()
        );
    }   
}
