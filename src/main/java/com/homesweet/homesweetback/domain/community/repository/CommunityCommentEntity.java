package com.homesweet.homesweetback.domain.community.repository;

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
 * CommunityComment 엔티티
 *
 * @author ohhalim777@gmail.com
 * @date 25. 10. 21.
 */

@Entity
@Table(name = "community_comments")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityCommentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long commentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private CommunityPostEntity post;

    @Column(name = "user_id", nullable = false)
    private Long userId;
    //    TODO: 유저 엔티티 만들어지면 수정
    //    @ManyToOne(fetch = FetchType.LAZY)
    //    @JoinColumn(name = "user_id", nullable = false)
    //    private UserEntity user;

    @Column(name = "parent_comment_id", columnDefinition = "BIGINT")
    private Long parentCommentId;

    @Column(nullable = false, length = 500)
    private String content;

    @Column(nullable = false)
    @Builder.Default
    private Integer likeCount = 0;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isModified = false;

    private LocalDateTime modifiedAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;
}
