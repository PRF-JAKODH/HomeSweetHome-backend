package com.homesweet.homesweetback.domain.settlement.batch.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

/**
 * 정산 배치(Job)의 전반적인 실행 흐름을 감시하는 리스너
 * Job 시작/종료 시점을 기록하고 전체 소요 시간을 로깅한다.
 * Job 실행 중 발생한 모든 Exception을 수집하여 로그로 출력한다.
 * - 정산 배치 전체 상태를 중앙에서 모니터링
 * - 장애 상황을 한 곳에서 파악할 수 있도록 통합 로그 제공
 */
@Slf4j
@Component
public class SettlementJobListener implements JobExecutionListener {
    private Long startTime;

    // job 실행 전
    @Override
    public void beforeJob(JobExecution jobExecution) {
        startTime = System.currentTimeMillis();
        log.info("[JOB 시작] {} (jobId={})", jobExecution.getJobInstance().getJobName(), jobExecution.getJobId());
    }

    // job 실행 후
    @Override
    public void afterJob(JobExecution jobExecution) {
        long duration = System.currentTimeMillis() - startTime;
        log.info("[JOB 종료] {} (jobId={}) / status= {} / duration= {}ms", jobExecution.getJobInstance().getJobName(), jobExecution.getJobId(), jobExecution.getStatus(), duration);
        if (!jobExecution.getAllFailureExceptions().isEmpty()) {
            jobExecution.getAllFailureExceptions().forEach(failureException -> {
                log.error("[JOB 예외 발생] {}", failureException.getMessage(), failureException);
            });
        }
    }
}
