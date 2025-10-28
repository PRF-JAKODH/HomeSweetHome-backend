package com.homesweet.homesweetback.domain.settlement.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QSettlement is a Querydsl query type for Settlement
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QSettlement extends EntityPathBase<Settlement> {

    private static final long serialVersionUID = -302640039L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QSettlement settlement = new QSettlement("settlement");

    public final NumberPath<java.math.BigDecimal> fee = createNumber("fee", java.math.BigDecimal.class);

    public final com.homesweet.homesweetback.domain.order.entity.QOrder order;

    public final BooleanPath orderCanceled = createBoolean("orderCanceled");

    public final NumberPath<java.math.BigDecimal> refundAmount = createNumber("refundAmount", java.math.BigDecimal.class);

    public final NumberPath<java.math.BigDecimal> salesAmount = createNumber("salesAmount", java.math.BigDecimal.class);

    public final NumberPath<java.math.BigDecimal> settlementAmount = createNumber("settlementAmount", java.math.BigDecimal.class);

    public final DateTimePath<java.time.LocalDateTime> settlementDate = createDateTime("settlementDate", java.time.LocalDateTime.class);

    public final NumberPath<Long> settlementId = createNumber("settlementId", Long.class);

    public final StringPath settlementStatus = createString("settlementStatus");

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public final NumberPath<java.math.BigDecimal> vat = createNumber("vat", java.math.BigDecimal.class);

    public QSettlement(String variable) {
        this(Settlement.class, forVariable(variable), INITS);
    }

    public QSettlement(Path<? extends Settlement> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QSettlement(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QSettlement(PathMetadata metadata, PathInits inits) {
        this(Settlement.class, metadata, inits);
    }

    public QSettlement(Class<? extends Settlement> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.order = inits.isInitialized("order") ? new com.homesweet.homesweetback.domain.order.entity.QOrder(forProperty("order")) : null;
    }

}

