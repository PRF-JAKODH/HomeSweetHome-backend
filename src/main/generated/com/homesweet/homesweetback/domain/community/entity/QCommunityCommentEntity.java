package com.homesweet.homesweetback.domain.community.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QCommunityCommentEntity is a Querydsl query type for CommunityCommentEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QCommunityCommentEntity extends EntityPathBase<CommunityCommentEntity> {

    private static final long serialVersionUID = -1135715051L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QCommunityCommentEntity communityCommentEntity = new QCommunityCommentEntity("communityCommentEntity");

    public final com.homesweet.homesweetback.domain.auth.entity.QUser author;

    public final NumberPath<Long> commentId = createNumber("commentId", Long.class);

    public final StringPath content = createString("content");

    public final BooleanPath isDeleted = createBoolean("isDeleted");

    public final BooleanPath isModified = createBoolean("isModified");

    public final NumberPath<Integer> likeCount = createNumber("likeCount", Integer.class);

    public final DateTimePath<java.time.LocalDateTime> modifiedAt = createDateTime("modifiedAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> parentCommentId = createNumber("parentCommentId", Long.class);

    public final QCommunityPostEntity post;

    public QCommunityCommentEntity(String variable) {
        this(CommunityCommentEntity.class, forVariable(variable), INITS);
    }

    public QCommunityCommentEntity(Path<? extends CommunityCommentEntity> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QCommunityCommentEntity(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QCommunityCommentEntity(PathMetadata metadata, PathInits inits) {
        this(CommunityCommentEntity.class, metadata, inits);
    }

    public QCommunityCommentEntity(Class<? extends CommunityCommentEntity> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.author = inits.isInitialized("author") ? new com.homesweet.homesweetback.domain.auth.entity.QUser(forProperty("author"), inits.get("author")) : null;
        this.post = inits.isInitialized("post") ? new QCommunityPostEntity(forProperty("post")) : null;
    }

}

