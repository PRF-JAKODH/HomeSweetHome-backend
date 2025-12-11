package com.homesweet.homesweetback.domain.settlement.batch.listener;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;


// Step과 Job 지표 수집
@Component
@RequiredArgsConstructor
public class BatchStepMetricsListener implements StepExecutionListener {
    private final MeterRegistry meterRegistry;

    @Override
    public void beforeStep(StepExecution stepExecution) {
        stepExecution.getExecutionContext().put("stepStart", System.currentTimeMillis());
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        long start = (long) stepExecution.getExecutionContext().get("stepStart");
        long duration = System.currentTimeMillis() - start;

        meterRegistry.timer("batch_step_duration",
                "step", stepExecution.getStepName()
        ).record(duration, TimeUnit.MILLISECONDS);

        meterRegistry.counter("batch_step_count", "step", stepExecution.getStepName()).increment();

        if (!stepExecution.getStatus().isUnsuccessful()) {
            meterRegistry.counter("batch_step_success", "step", stepExecution.getStepName()).increment();
        } else {
            meterRegistry.counter("batch_step_failure", "step", stepExecution.getStepName()).increment();
        }

        return stepExecution.getExitStatus();
    }
}
