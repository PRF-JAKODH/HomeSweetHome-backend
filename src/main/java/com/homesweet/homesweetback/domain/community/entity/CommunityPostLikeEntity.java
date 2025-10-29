package com.homesweet.homesweetback.domain.community.entity;

import com.homesweet.homesweetback.domain.auth.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * CommunityPostLike 엔티티
 *
 * @author ohhalim777@gmail.com
 * @date 25. 10. 21.
 */

@Entity
@Table(
        name = "posts_likes",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"post_id", "user_id"}
        )
)
@IdClass(CommunityPostLikeId.class)  // 복합키 클래스 지정
@EntityListeners(AuditingEntityListener.class)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityPostLikeEntity {

    @Id  
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private CommunityPostEntity post;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
