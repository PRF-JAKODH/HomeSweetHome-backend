package com.homesweet.homesweetback.domain.auth.dto;

public record SignUpResponse(
    String accessToken,
    UserResponse userResponse
) {
}
