package com.homesweet.homesweetback.domain.community.repository;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * CommunityCommentLike 엔티티
 *
 * @author ohhalim777@gmail.com
 * @date 25. 10. 21.
 */

@Entity
@Table(
        name = "community_comment_likes",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"comment_id", "user_id"}
        )
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityCommentLikeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long likeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", nullable = false)
    private CommunityCommentEntity comment;

    @Column(name = "user_id", nullable = false)
    private Long userId;
    //    TODO: 유저 엔티티 만들어지면 수정
    //    @ManyToOne(fetch = FetchType.LAZY)
    //    @JoinColumn(name = "user_id", nullable = false)
    //    private UserEntity user;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
