package com.homesweet.homesweetback.domain.community.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
// 복합키
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class CommunityCommentLikeId implements Serializable {
    private Long comment;
    private Long user;
}