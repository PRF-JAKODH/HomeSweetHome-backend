package com.homesweet.homesweetback.domain.product.product.repository.jpa.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QProductOptionValueEntity is a Querydsl query type for ProductOptionValueEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QProductOptionValueEntity extends EntityPathBase<ProductOptionValueEntity> {

    private static final long serialVersionUID = 816220876L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QProductOptionValueEntity productOptionValueEntity = new QProductOptionValueEntity("productOptionValueEntity");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final QProductOptionGroupEntity group;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final ListPath<ProductSkuOptionEntity, QProductSkuOptionEntity> skuLinks = this.<ProductSkuOptionEntity, QProductSkuOptionEntity>createList("skuLinks", ProductSkuOptionEntity.class, QProductSkuOptionEntity.class, PathInits.DIRECT2);

    public final StringPath value = createString("value");

    public QProductOptionValueEntity(String variable) {
        this(ProductOptionValueEntity.class, forVariable(variable), INITS);
    }

    public QProductOptionValueEntity(Path<? extends ProductOptionValueEntity> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QProductOptionValueEntity(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QProductOptionValueEntity(PathMetadata metadata, PathInits inits) {
        this(ProductOptionValueEntity.class, metadata, inits);
    }

    public QProductOptionValueEntity(Class<? extends ProductOptionValueEntity> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.group = inits.isInitialized("group") ? new QProductOptionGroupEntity(forProperty("group"), inits.get("group")) : null;
    }

}

