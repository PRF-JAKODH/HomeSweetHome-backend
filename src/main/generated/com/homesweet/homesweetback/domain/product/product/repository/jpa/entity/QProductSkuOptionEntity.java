package com.homesweet.homesweetback.domain.product.product.repository.jpa.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QProductSkuOptionEntity is a Querydsl query type for ProductSkuOptionEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QProductSkuOptionEntity extends EntityPathBase<ProductSkuOptionEntity> {

    private static final long serialVersionUID = -975054974L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QProductSkuOptionEntity productSkuOptionEntity = new QProductSkuOptionEntity("productSkuOptionEntity");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final QProductOptionValueEntity optionValue;

    public final QSkuEntity sku;

    public QProductSkuOptionEntity(String variable) {
        this(ProductSkuOptionEntity.class, forVariable(variable), INITS);
    }

    public QProductSkuOptionEntity(Path<? extends ProductSkuOptionEntity> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QProductSkuOptionEntity(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QProductSkuOptionEntity(PathMetadata metadata, PathInits inits) {
        this(ProductSkuOptionEntity.class, metadata, inits);
    }

    public QProductSkuOptionEntity(Class<? extends ProductSkuOptionEntity> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.optionValue = inits.isInitialized("optionValue") ? new QProductOptionValueEntity(forProperty("optionValue"), inits.get("optionValue")) : null;
        this.sku = inits.isInitialized("sku") ? new QSkuEntity(forProperty("sku"), inits.get("sku")) : null;
    }

}

