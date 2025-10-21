package com.homesweet.homesweetback.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;

/**
 * 회원가입 완료를 위한 추가 정보 입력 DTO
 */
public record SignupRequest(
    @NotBlank(message = "핸드폰 번호는 필수입니다")
    @Pattern(regexp = "^01[0-9]-[0-9]{3,4}-[0-9]{4}$", 
             message = "올바른 핸드폰 번호 형식이 아닙니다 (예: 010-1234-5678)")
    String phoneNumber,
    
    @NotNull(message = "생일은 필수입니다")
    LocalDate birthDate
) {
}
