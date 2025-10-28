package com.homesweet.homesweetback.domain.settlement.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QWeeklySettlement is a Querydsl query type for WeeklySettlement
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QWeeklySettlement extends EntityPathBase<WeeklySettlement> {

    private static final long serialVersionUID = 1703698362L;

    public static final QWeeklySettlement weeklySettlement = new QWeeklySettlement("weeklySettlement");

    public final NumberPath<java.math.BigDecimal> dailySales = createNumber("dailySales", java.math.BigDecimal.class);

    public final NumberPath<Byte> month = createNumber("month", Byte.class);

    public final NumberPath<java.math.BigDecimal> totalFee = createNumber("totalFee", java.math.BigDecimal.class);

    public final NumberPath<java.math.BigDecimal> totalRefund = createNumber("totalRefund", java.math.BigDecimal.class);

    public final NumberPath<java.math.BigDecimal> totalSales = createNumber("totalSales", java.math.BigDecimal.class);

    public final NumberPath<java.math.BigDecimal> totalSettlement = createNumber("totalSettlement", java.math.BigDecimal.class);

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public final DatePath<java.time.LocalDate> weekEndDate = createDate("weekEndDate", java.time.LocalDate.class);

    public final NumberPath<Long> weeklyId = createNumber("weeklyId", Long.class);

    public final NumberPath<java.math.BigDecimal> weeklySales = createNumber("weeklySales", java.math.BigDecimal.class);

    public final DatePath<java.time.LocalDate> weekStartDate = createDate("weekStartDate", java.time.LocalDate.class);

    public final NumberPath<Short> year = createNumber("year", Short.class);

    public QWeeklySettlement(String variable) {
        super(WeeklySettlement.class, forVariable(variable));
    }

    public QWeeklySettlement(Path<? extends WeeklySettlement> path) {
        super(path.getType(), path.getMetadata());
    }

    public QWeeklySettlement(PathMetadata metadata) {
        super(WeeklySettlement.class, metadata);
    }

}

