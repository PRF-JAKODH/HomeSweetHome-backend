package com.homesweet.homesweetback.domain.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Hello 생성 요청 DTO
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 18.
 */
public record CommunityCreateRequest(
        @NotBlank(message = "name은 필수입니다.")
        @Size(max = 20, message = "name은 20자 이하이어야 합니다.")
        String name
) {}
