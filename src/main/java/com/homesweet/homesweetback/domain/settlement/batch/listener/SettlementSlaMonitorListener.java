package com.homesweet.homesweetback.domain.settlement.batch.listener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.annotation.AfterStep;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.stereotype.Component;

/**
 *  각 Step의 실행 시간을 측정하고
 *  설정된 SLA(10초)를 넘는지 자동 모니터링하며
 *  로그로 알림까지 남기는 Listener.
 *
 * ⚡ 적용 이유:
 *   - 정산 시스템은 실시간/준실시간 요구사항(SLA 10초)이 강하게 존재함.
 *   - 각 Step이 목표 시간을 준수하는지 지속적으로 추적할 필요가 있음.
 *   - 성능 저하, 병목 구간, SQL 지연 등을 빠르게 탐지하기 위함.
 */
@Slf4j
@Component
public class SettlementSlaMonitorListener implements StepExecutionListener {
    private static final long SLA_MILLIS = 10_000;

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        stepExecution.getExecutionContext().putLong("SLAStartTime", System.currentTimeMillis());
    }
    @AfterStep
    public ExitStatus afterStep(StepExecution stepExecution) {
        long start = stepExecution.getExecutionContext().getLong("SLAStartTime");
        long duration = System.currentTimeMillis() - start;
        log.info("[SLA 모니터링] step= {} 실행시간= {}ms ({}초)", stepExecution.getStepName(), duration, duration / 1000.0);
        if (duration > SLA_MILLIS) {
            log.warn("[SLA 경고] step= {} 실행시간 초과 {}ms > {}ms", stepExecution.getStepName(), duration, SLA_MILLIS);
        }
        return stepExecution.getExitStatus();
    }
}
