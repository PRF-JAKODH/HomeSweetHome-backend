package com.homesweet.homesweetback.domain.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Community 생성 요청 DTO
 *
 * @author ohhalim777@gmail.com
 * @date 25. 10. 21.
 */
public record CommunityCreateRequest(
        // TODO: 실제 필드 정의
        String placeholder
) {}
