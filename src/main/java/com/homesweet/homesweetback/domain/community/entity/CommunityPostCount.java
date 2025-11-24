package com.homesweet.homesweetback.domain.community.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 게시글 카운트 전용 엔티티 (락 분리를 위한 테이블 분리)
 * - community_posts와 1:1 관계
 * - post_id를 PK로 사용하여 조인 최적화
 * - 카운트 업데이트 시 게시글 본문과 독립적인 락 획득
 *
 * @author ohhalim777@gmail.com
 * @date 25. 11. 24.
 */
@Entity
@Table(name = "community_post_counts")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityPostCount {

    @Id
    @Column(name = "post_id")
    private Long postId;

    @Column(nullable = false)
    @Builder.Default
    private Integer viewCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer likeCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer commentCount = 0;

    /**
     * 새 게시글 카운트 초기화
     */
    public static CommunityPostCount createFor(Long postId) {
        return CommunityPostCount.builder()
                .postId(postId)
                .viewCount(0)
                .likeCount(0)
                .commentCount(0)
                .build();
    }
}
