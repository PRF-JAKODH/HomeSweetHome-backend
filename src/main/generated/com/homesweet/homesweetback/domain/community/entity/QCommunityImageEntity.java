package com.homesweet.homesweetback.domain.community.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QCommunityImageEntity is a Querydsl query type for CommunityImageEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QCommunityImageEntity extends EntityPathBase<CommunityImageEntity> {

    private static final long serialVersionUID = 1372048337L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QCommunityImageEntity communityImageEntity = new QCommunityImageEntity("communityImageEntity");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> imageId = createNumber("imageId", Long.class);

    public final NumberPath<Integer> imageOrder = createNumber("imageOrder", Integer.class);

    public final StringPath imageUrl = createString("imageUrl");

    public final QCommunityPostEntity post;

    public QCommunityImageEntity(String variable) {
        this(CommunityImageEntity.class, forVariable(variable), INITS);
    }

    public QCommunityImageEntity(Path<? extends CommunityImageEntity> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QCommunityImageEntity(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QCommunityImageEntity(PathMetadata metadata, PathInits inits) {
        this(CommunityImageEntity.class, metadata, inits);
    }

    public QCommunityImageEntity(Class<? extends CommunityImageEntity> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.post = inits.isInitialized("post") ? new QCommunityPostEntity(forProperty("post")) : null;
    }

}

