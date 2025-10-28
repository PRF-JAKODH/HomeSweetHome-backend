package com.homesweet.homesweetback.domain.community.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QCommunityCommentLikeEntity is a Querydsl query type for CommunityCommentLikeEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QCommunityCommentLikeEntity extends EntityPathBase<CommunityCommentLikeEntity> {

    private static final long serialVersionUID = 42975884L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QCommunityCommentLikeEntity communityCommentLikeEntity = new QCommunityCommentLikeEntity("communityCommentLikeEntity");

    public final QCommunityCommentEntity comment;

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> likeId = createNumber("likeId", Long.class);

    public final com.homesweet.homesweetback.domain.auth.entity.QUser user;

    public QCommunityCommentLikeEntity(String variable) {
        this(CommunityCommentLikeEntity.class, forVariable(variable), INITS);
    }

    public QCommunityCommentLikeEntity(Path<? extends CommunityCommentLikeEntity> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QCommunityCommentLikeEntity(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QCommunityCommentLikeEntity(PathMetadata metadata, PathInits inits) {
        this(CommunityCommentLikeEntity.class, metadata, inits);
    }

    public QCommunityCommentLikeEntity(Class<? extends CommunityCommentLikeEntity> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.comment = inits.isInitialized("comment") ? new QCommunityCommentEntity(forProperty("comment"), inits.get("comment")) : null;
        this.user = inits.isInitialized("user") ? new com.homesweet.homesweetback.domain.auth.entity.QUser(forProperty("user"), inits.get("user")) : null;
    }

}

