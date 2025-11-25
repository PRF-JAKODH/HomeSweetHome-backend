package com.homesweet.homesweetback.domain.settlement.batch.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.stereotype.Component;

/**
 * Chunk 단위별로 처리 시간을 기록하고, 오류 상황을 로깅하는 Listener.
 * 사용 목적:
 * Chunk 단위 처리 시간(SLA) 모니터링
 * Chunk 실패 시 로깅
 * Step 단위의 ExecutionContext를 활용해 상태 공유
 */
@Slf4j
@Component
public class SettlementChunkListener implements ChunkListener {
    @Override
    public void beforeChunk(ChunkContext chunkContext) {
        StepExecution stepExecution = chunkContext.getStepContext().getStepExecution();
        stepExecution.getExecutionContext().putLong("chunkStart", System.currentTimeMillis());
    }
    @Override
    public void afterChunk(ChunkContext chunkContext) {
        StepExecution stepExecution = chunkContext.getStepContext().getStepExecution();
        long start = stepExecution.getExecutionContext().getLong("chunkStart");
        long duration = System.currentTimeMillis() - start;
        log.info("Chunk process {}ms", duration);
    }
    @Override
    public void afterChunkError(ChunkContext chunkContext) {
        log.error("Chunk Error");
    }
}
