package com.homesweet.homesweetback.domain.community.entity;

import com.homesweet.homesweetback.common.BaseEntity;
import com.homesweet.homesweetback.domain.auth.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * CommunityPost 엔티티
 *
 * @author ohhalim777@gmail.com
 * @date 25. 10. 20.
 */

@Entity
@Table(name = "community_posts")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityPostEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long postId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User author;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(nullable = false)
    @Builder.Default
    private Integer viewCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer likeCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer commentCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isModified = false;

    private LocalDateTime modifiedAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;
}
