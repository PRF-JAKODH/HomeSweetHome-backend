package com.homesweet.homesweetback.domain.product.product.repository.jpa.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QSkuEntity is a Querydsl query type for SkuEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QSkuEntity extends EntityPathBase<SkuEntity> {

    private static final long serialVersionUID = 378399716L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QSkuEntity skuEntity = new QSkuEntity("skuEntity");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Integer> priceAdjustment = createNumber("priceAdjustment", Integer.class);

    public final QProductEntity product;

    public final ListPath<ProductSkuOptionEntity, QProductSkuOptionEntity> skuOptions = this.<ProductSkuOptionEntity, QProductSkuOptionEntity>createList("skuOptions", ProductSkuOptionEntity.class, QProductSkuOptionEntity.class, PathInits.DIRECT2);

    public final NumberPath<Long> stockQuantity = createNumber("stockQuantity", Long.class);

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public QSkuEntity(String variable) {
        this(SkuEntity.class, forVariable(variable), INITS);
    }

    public QSkuEntity(Path<? extends SkuEntity> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QSkuEntity(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QSkuEntity(PathMetadata metadata, PathInits inits) {
        this(SkuEntity.class, metadata, inits);
    }

    public QSkuEntity(Class<? extends SkuEntity> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.product = inits.isInitialized("product") ? new QProductEntity(forProperty("product"), inits.get("product")) : null;
    }

}

