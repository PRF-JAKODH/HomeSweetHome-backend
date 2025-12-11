package com.homesweet.homesweetback.domain.settlement.batch.listener;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class BatchJobMetricsListener implements JobExecutionListener {

    private final MeterRegistry meterRegistry;

    @Override
    public void beforeJob(JobExecution jobExecution) {
        jobExecution.getExecutionContext().put("jobStart", System.currentTimeMillis());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        long start = (long) jobExecution.getExecutionContext().get("jobStart");
        long duration = System.currentTimeMillis() - start;

        meterRegistry.timer("batch_job_duration",
                "job", jobExecution.getJobInstance().getJobName()
        ).record(duration, TimeUnit.MILLISECONDS);

        meterRegistry.counter("batch_job_count", "job", jobExecution.getJobInstance().getJobName()).increment();

        if (jobExecution.getStatus().isUnsuccessful()) {
            meterRegistry.counter("batch_job_failure", "job", jobExecution.getJobInstance().getJobName()).increment();
        } else {
            meterRegistry.counter("batch_job_success", "job", jobExecution.getJobInstance().getJobName()).increment();
        }
    }

}
