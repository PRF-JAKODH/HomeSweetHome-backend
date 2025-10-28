package com.homesweet.homesweetback.domain.settlement.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QYearlySettlement is a Querydsl query type for YearlySettlement
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QYearlySettlement extends EntityPathBase<YearlySettlement> {

    private static final long serialVersionUID = 651043459L;

    public static final QYearlySettlement yearlySettlement = new QYearlySettlement("yearlySettlement");

    public final NumberPath<java.math.BigDecimal> totalFee = createNumber("totalFee", java.math.BigDecimal.class);

    public final NumberPath<java.math.BigDecimal> totalRefund = createNumber("totalRefund", java.math.BigDecimal.class);

    public final NumberPath<java.math.BigDecimal> totalSales = createNumber("totalSales", java.math.BigDecimal.class);

    public final NumberPath<java.math.BigDecimal> totalSettlement = createNumber("totalSettlement", java.math.BigDecimal.class);

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public final NumberPath<Short> year = createNumber("year", Short.class);

    public final NumberPath<Long> yearlyId = createNumber("yearlyId", Long.class);

    public QYearlySettlement(String variable) {
        super(YearlySettlement.class, forVariable(variable));
    }

    public QYearlySettlement(Path<? extends YearlySettlement> path) {
        super(path.getType(), path.getMetadata());
    }

    public QYearlySettlement(PathMetadata metadata) {
        super(YearlySettlement.class, metadata);
    }

}

