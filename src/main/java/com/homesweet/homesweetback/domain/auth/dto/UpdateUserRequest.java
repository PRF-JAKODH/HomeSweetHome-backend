package com.homesweet.homesweetback.domain.auth.dto;

import java.time.LocalDate;

public record UpdateUserRequest(
    String name,
    String phoneNumber,
    LocalDate birthDate,
    String address
) {
}
