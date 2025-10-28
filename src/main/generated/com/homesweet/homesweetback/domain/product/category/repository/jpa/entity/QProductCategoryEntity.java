package com.homesweet.homesweetback.domain.product.category.repository.jpa.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QProductCategoryEntity is a Querydsl query type for ProductCategoryEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QProductCategoryEntity extends EntityPathBase<ProductCategoryEntity> {

    private static final long serialVersionUID = 790171925L;

    public static final QProductCategoryEntity productCategoryEntity = new QProductCategoryEntity("productCategoryEntity");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Integer> depth = createNumber("depth", Integer.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath name = createString("name");

    public final NumberPath<Long> parentId = createNumber("parentId", Long.class);

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public QProductCategoryEntity(String variable) {
        super(ProductCategoryEntity.class, forVariable(variable));
    }

    public QProductCategoryEntity(Path<? extends ProductCategoryEntity> path) {
        super(path.getType(), path.getMetadata());
    }

    public QProductCategoryEntity(PathMetadata metadata) {
        super(ProductCategoryEntity.class, metadata);
    }

}

