package com.homesweet.homesweetback.domain.settlement.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QDailySettlement is a Querydsl query type for DailySettlement
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QDailySettlement extends EntityPathBase<DailySettlement> {

    private static final long serialVersionUID = -669570318L;

    public static final QDailySettlement dailySettlement = new QDailySettlement("dailySettlement");

    public final NumberPath<Long> dailyId = createNumber("dailyId", Long.class);

    public final DateTimePath<java.time.LocalDateTime> settlementDate = createDateTime("settlementDate", java.time.LocalDateTime.class);

    public final NumberPath<java.math.BigDecimal> totalFee = createNumber("totalFee", java.math.BigDecimal.class);

    public final NumberPath<java.math.BigDecimal> totalRefund = createNumber("totalRefund", java.math.BigDecimal.class);

    public final NumberPath<java.math.BigDecimal> totalSales = createNumber("totalSales", java.math.BigDecimal.class);

    public final NumberPath<java.math.BigDecimal> totalSettlement = createNumber("totalSettlement", java.math.BigDecimal.class);

    public final NumberPath<java.math.BigDecimal> totalVat = createNumber("totalVat", java.math.BigDecimal.class);

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QDailySettlement(String variable) {
        super(DailySettlement.class, forVariable(variable));
    }

    public QDailySettlement(Path<? extends DailySettlement> path) {
        super(path.getType(), path.getMetadata());
    }

    public QDailySettlement(PathMetadata metadata) {
        super(DailySettlement.class, metadata);
    }

}

