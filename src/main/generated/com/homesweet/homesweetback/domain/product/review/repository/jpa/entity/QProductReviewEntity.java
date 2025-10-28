package com.homesweet.homesweetback.domain.product.review.repository.jpa.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QProductReviewEntity is a Querydsl query type for ProductReviewEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QProductReviewEntity extends EntityPathBase<ProductReviewEntity> {

    private static final long serialVersionUID = 640298249L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QProductReviewEntity productReviewEntity = new QProductReviewEntity("productReviewEntity");

    public final StringPath comment = createString("comment");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath imageUrl = createString("imageUrl");

    public final com.homesweet.homesweetback.domain.product.product.repository.jpa.entity.QProductEntity product;

    public final NumberPath<Integer> rating = createNumber("rating", Integer.class);

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public final com.homesweet.homesweetback.domain.auth.entity.QUser user;

    public QProductReviewEntity(String variable) {
        this(ProductReviewEntity.class, forVariable(variable), INITS);
    }

    public QProductReviewEntity(Path<? extends ProductReviewEntity> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QProductReviewEntity(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QProductReviewEntity(PathMetadata metadata, PathInits inits) {
        this(ProductReviewEntity.class, metadata, inits);
    }

    public QProductReviewEntity(Class<? extends ProductReviewEntity> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.product = inits.isInitialized("product") ? new com.homesweet.homesweetback.domain.product.product.repository.jpa.entity.QProductEntity(forProperty("product"), inits.get("product")) : null;
        this.user = inits.isInitialized("user") ? new com.homesweet.homesweetback.domain.auth.entity.QUser(forProperty("user"), inits.get("user")) : null;
    }

}

