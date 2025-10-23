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
        @NotBlank(message = "제목은 필수입니다")
        @Size(min = 1, max = 100, message = "제목은 1자 이상 100자 이하여야 합니다")
        String title,

        @NotBlank(message = "내용은 필수입니다")
        @Size(min = 1, max = 1000, message = "내용은 1자 이상 1000자 이하여야 합니다")
        String content
) {}
