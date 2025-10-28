package com.homesweet.homesweetback.domain.product.product.repository.jpa.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QProductEntity is a Querydsl query type for ProductEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QProductEntity extends EntityPathBase<ProductEntity> {

    private static final long serialVersionUID = -1556122570L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QProductEntity productEntity = new QProductEntity("productEntity");

    public final NumberPath<Integer> basePrice = createNumber("basePrice", Integer.class);

    public final StringPath brand = createString("brand");

    public final com.homesweet.homesweetback.domain.product.category.repository.jpa.entity.QProductCategoryEntity category;

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final StringPath description = createString("description");

    public final ListPath<ProductDetailImageEntity, QProductDetailImageEntity> detailImages = this.<ProductDetailImageEntity, QProductDetailImageEntity>createList("detailImages", ProductDetailImageEntity.class, QProductDetailImageEntity.class, PathInits.DIRECT2);

    public final NumberPath<java.math.BigDecimal> discountRate = createNumber("discountRate", java.math.BigDecimal.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath imageUrl = createString("imageUrl");

    public final StringPath name = createString("name");

    public final ListPath<ProductOptionGroupEntity, QProductOptionGroupEntity> optionGroups = this.<ProductOptionGroupEntity, QProductOptionGroupEntity>createList("optionGroups", ProductOptionGroupEntity.class, QProductOptionGroupEntity.class, PathInits.DIRECT2);

    public final com.homesweet.homesweetback.domain.auth.entity.QUser seller;

    public final NumberPath<Integer> shippingPrice = createNumber("shippingPrice", Integer.class);

    public final ListPath<SkuEntity, QSkuEntity> skus = this.<SkuEntity, QSkuEntity>createList("skus", SkuEntity.class, QSkuEntity.class, PathInits.DIRECT2);

    public final EnumPath<com.homesweet.homesweetback.domain.product.product.domain.ProductStatus> status = createEnum("status", com.homesweet.homesweetback.domain.product.product.domain.ProductStatus.class);

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public QProductEntity(String variable) {
        this(ProductEntity.class, forVariable(variable), INITS);
    }

    public QProductEntity(Path<? extends ProductEntity> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QProductEntity(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QProductEntity(PathMetadata metadata, PathInits inits) {
        this(ProductEntity.class, metadata, inits);
    }

    public QProductEntity(Class<? extends ProductEntity> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.category = inits.isInitialized("category") ? new com.homesweet.homesweetback.domain.product.category.repository.jpa.entity.QProductCategoryEntity(forProperty("category")) : null;
        this.seller = inits.isInitialized("seller") ? new com.homesweet.homesweetback.domain.auth.entity.QUser(forProperty("seller"), inits.get("seller")) : null;
    }

}

