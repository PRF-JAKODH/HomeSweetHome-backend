package com.homesweet.homesweetback.domain.settlement.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QMonthlySettlement is a Querydsl query type for MonthlySettlement
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMonthlySettlement extends EntityPathBase<MonthlySettlement> {

    private static final long serialVersionUID = -537032730L;

    public static final QMonthlySettlement monthlySettlement = new QMonthlySettlement("monthlySettlement");

    public final NumberPath<Byte> month = createNumber("month", Byte.class);

    public final NumberPath<Long> monthlyId = createNumber("monthlyId", Long.class);

    public final NumberPath<java.math.BigDecimal> totalFee = createNumber("totalFee", java.math.BigDecimal.class);

    public final NumberPath<java.math.BigDecimal> totalRefund = createNumber("totalRefund", java.math.BigDecimal.class);

    public final NumberPath<java.math.BigDecimal> totalSales = createNumber("totalSales", java.math.BigDecimal.class);

    public final NumberPath<java.math.BigDecimal> totalSettlement = createNumber("totalSettlement", java.math.BigDecimal.class);

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public final NumberPath<Short> year = createNumber("year", Short.class);

    public QMonthlySettlement(String variable) {
        super(MonthlySettlement.class, forVariable(variable));
    }

    public QMonthlySettlement(Path<? extends MonthlySettlement> path) {
        super(path.getType(), path.getMetadata());
    }

    public QMonthlySettlement(PathMetadata metadata) {
        super(MonthlySettlement.class, metadata);
    }

}

