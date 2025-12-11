package com.homesweet.homesweetback.domain.settlement.batch.step.create;

import com.homesweet.homesweetback.domain.order.entity.OrderStatus;
import com.homesweet.homesweetback.domain.settlement.dto.response.SettlementCreateDto;
import com.homesweet.homesweetback.domain.settlement.repository.querydsl.CustomSettlementRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@StepScope
@RequiredArgsConstructor
public class ZeroOffsetItemReader implements ItemReader<SettlementCreateDto> {
    private final CustomSettlementRepository customSettlementRepository;
    private final LocalDateTime cutoff;
    private final MeterRegistry meterRegistry;

    private static final int PAGE_SIZE = 1000;

    private boolean initialized = false;
    private Long lastId = 0L;

    private List<SettlementCreateDto> buffer = new ArrayList<>();
    private int bufferIdx = 0;

    public ZeroOffsetItemReader(
            @Value("#{jobParameters['cutoff']}") String cutoffString,
            CustomSettlementRepository customSettlementRepository,
            MeterRegistry meterRegistry
    ) {
        this.customSettlementRepository = customSettlementRepository;
        LocalDateTime parsed = LocalDateTime.parse(cutoffString);
        this.cutoff = parsed.withNano(0);
        this.meterRegistry = meterRegistry;

    }

    @Override
    @Transactional(readOnly = true)
    public SettlementCreateDto read() {
        long start = System.currentTimeMillis();

        try {
            if (!initialized) {
                log.info("[ZeroOffsetItemReader] 최초 실행 cutoff={}", cutoff);
                initialized = true;
            }

            if (bufferIdx >= buffer.size()) {
                fillBuffer();
                if (buffer.isEmpty()) {
                    return null;
                }
            }

            SettlementCreateDto dto = buffer.get(bufferIdx++);
            meterRegistry.counter("batch_reader_item_count").increment();

            return dto;

        } finally {
            long duration = System.currentTimeMillis() - start;
            meterRegistry.timer("batch_reader_read_duration",
                            Tags.of("reader", "ZeroOffsetItemReader"))
                    .record(duration, java.util.concurrent.TimeUnit.MILLISECONDS);
        }
    }

    private void fillBuffer() {
        long startFill = System.currentTimeMillis();
        try {
            // 1) ID 조회 시간 측정
            long startIds = System.currentTimeMillis();
            List<Long> ids = customSettlementRepository.findUnsettledOrderIds(
                    OrderStatus.COMPLETED,
                    cutoff,
                    lastId,
                    PAGE_SIZE
            );
            log.info("ids={}",ids.toString());
            long idDuration = System.currentTimeMillis() - startIds;
            meterRegistry.timer("batch_reader_query_ids_duration",
                            Tags.of("reader", "ZeroOffsetItemReader"))
                    .record(idDuration, java.util.concurrent.TimeUnit.MILLISECONDS);


            if (ids.isEmpty()) {
                buffer = List.of();
                return;
            }

            // 2) JOIN 조회 시간 측정
            long startJoin = System.currentTimeMillis();
            buffer = customSettlementRepository.findOrdersByIds(ids);
            long joinDuration = System.currentTimeMillis() - startJoin;
            meterRegistry.timer("batch_reader_query_join_duration",
                            Tags.of("reader", "ZeroOffsetItemReader"))
                    .record(joinDuration, java.util.concurrent.TimeUnit.MILLISECONDS);


            // cursor 업데이트
            lastId = ids.get(ids.size() - 1);
            bufferIdx = 0;

            log.info("[ZeroOffsetItemReader] Loaded chunk: size={} lastId={}", buffer.size(), lastId);

            // buffer size gauge 기록
            meterRegistry.gauge(
                    "batch_reader_buffer_size",
                    Tags.of("reader", "zero_offset"),
                    buffer,
                    List::size
            );


        } finally {
            long duration = System.currentTimeMillis() - startFill;
            meterRegistry.timer("batch_reader_buffer_load_duration",
                            Tags.of("reader", "ZeroOffsetItemReader"))
                    .record(duration, java.util.concurrent.TimeUnit.MILLISECONDS);
        }
    }
}
