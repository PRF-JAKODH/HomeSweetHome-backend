package com.homesweet.homesweetback.domain.product.product.repository.jpa.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QProductDetailImageEntity is a Querydsl query type for ProductDetailImageEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QProductDetailImageEntity extends EntityPathBase<ProductDetailImageEntity> {

    private static final long serialVersionUID = 692369882L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QProductDetailImageEntity productDetailImageEntity = new QProductDetailImageEntity("productDetailImageEntity");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath imageUrl = createString("imageUrl");

    public final QProductEntity product;

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public QProductDetailImageEntity(String variable) {
        this(ProductDetailImageEntity.class, forVariable(variable), INITS);
    }

    public QProductDetailImageEntity(Path<? extends ProductDetailImageEntity> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QProductDetailImageEntity(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QProductDetailImageEntity(PathMetadata metadata, PathInits inits) {
        this(ProductDetailImageEntity.class, metadata, inits);
    }

    public QProductDetailImageEntity(Class<? extends ProductDetailImageEntity> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.product = inits.isInitialized("product") ? new QProductEntity(forProperty("product"), inits.get("product")) : null;
    }

}

