package com.homesweet.homesweetback.domain.product.product.repository.jpa.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QProductOptionGroupEntity is a Querydsl query type for ProductOptionGroupEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QProductOptionGroupEntity extends EntityPathBase<ProductOptionGroupEntity> {

    private static final long serialVersionUID = -1294832294L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QProductOptionGroupEntity productOptionGroupEntity = new QProductOptionGroupEntity("productOptionGroupEntity");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final StringPath groupName = createString("groupName");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final QProductEntity product;

    public final ListPath<ProductOptionValueEntity, QProductOptionValueEntity> values = this.<ProductOptionValueEntity, QProductOptionValueEntity>createList("values", ProductOptionValueEntity.class, QProductOptionValueEntity.class, PathInits.DIRECT2);

    public QProductOptionGroupEntity(String variable) {
        this(ProductOptionGroupEntity.class, forVariable(variable), INITS);
    }

    public QProductOptionGroupEntity(Path<? extends ProductOptionGroupEntity> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QProductOptionGroupEntity(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QProductOptionGroupEntity(PathMetadata metadata, PathInits inits) {
        this(ProductOptionGroupEntity.class, metadata, inits);
    }

    public QProductOptionGroupEntity(Class<? extends ProductOptionGroupEntity> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.product = inits.isInitialized("product") ? new QProductEntity(forProperty("product"), inits.get("product")) : null;
    }

}

