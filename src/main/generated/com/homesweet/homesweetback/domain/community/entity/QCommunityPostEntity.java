package com.homesweet.homesweetback.domain.community.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QCommunityPostEntity is a Querydsl query type for CommunityPostEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QCommunityPostEntity extends EntityPathBase<CommunityPostEntity> {

    private static final long serialVersionUID = -1432368240L;

    public static final QCommunityPostEntity communityPostEntity = new QCommunityPostEntity("communityPostEntity");

    public final NumberPath<Integer> commentCount = createNumber("commentCount", Integer.class);

    public final StringPath content = createString("content");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final BooleanPath isDeleted = createBoolean("isDeleted");

    public final BooleanPath isModified = createBoolean("isModified");

    public final NumberPath<Integer> likeCount = createNumber("likeCount", Integer.class);

    public final DateTimePath<java.time.LocalDateTime> modifiedAt = createDateTime("modifiedAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> postId = createNumber("postId", Long.class);

    public final StringPath title = createString("title");

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public final NumberPath<Integer> viewCount = createNumber("viewCount", Integer.class);

    public QCommunityPostEntity(String variable) {
        super(CommunityPostEntity.class, forVariable(variable));
    }

    public QCommunityPostEntity(Path<? extends CommunityPostEntity> path) {
        super(path.getType(), path.getMetadata());
    }

    public QCommunityPostEntity(PathMetadata metadata) {
        super(CommunityPostEntity.class, metadata);
    }

}

