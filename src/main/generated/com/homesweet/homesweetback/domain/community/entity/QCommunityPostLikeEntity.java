package com.homesweet.homesweetback.domain.community.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QCommunityPostLikeEntity is a Querydsl query type for CommunityPostLikeEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QCommunityPostLikeEntity extends EntityPathBase<CommunityPostLikeEntity> {

    private static final long serialVersionUID = 1967094663L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QCommunityPostLikeEntity communityPostLikeEntity = new QCommunityPostLikeEntity("communityPostLikeEntity");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> likeId = createNumber("likeId", Long.class);

    public final QCommunityPostEntity post;

    public final com.homesweet.homesweetback.domain.auth.entity.QUser user;

    public QCommunityPostLikeEntity(String variable) {
        this(CommunityPostLikeEntity.class, forVariable(variable), INITS);
    }

    public QCommunityPostLikeEntity(Path<? extends CommunityPostLikeEntity> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QCommunityPostLikeEntity(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QCommunityPostLikeEntity(PathMetadata metadata, PathInits inits) {
        this(CommunityPostLikeEntity.class, metadata, inits);
    }

    public QCommunityPostLikeEntity(Class<? extends CommunityPostLikeEntity> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.post = inits.isInitialized("post") ? new QCommunityPostEntity(forProperty("post")) : null;
        this.user = inits.isInitialized("user") ? new com.homesweet.homesweetback.domain.auth.entity.QUser(forProperty("user"), inits.get("user")) : null;
    }

}

