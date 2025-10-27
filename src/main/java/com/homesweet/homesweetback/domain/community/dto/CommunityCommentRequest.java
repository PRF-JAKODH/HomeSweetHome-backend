package com.homesweet.homesweetback.domain.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Community 댓글 작성/수정 요청 DTO
 *
 * @author ohhalim777@gmail.com
 * @date 25. 10. 21.
 */
public record CommunityCommentRequest(
        @NotBlank(message = "댓글 내용은 필수입니다")
        @Size(min = 1, max = 500, message = "댓글은 1자 이상 500자 이하여야 합니다")
        String content,

        Long parentCommentId  // 대댓글인 경우 부모 댓글 ID (선택)
) {}
