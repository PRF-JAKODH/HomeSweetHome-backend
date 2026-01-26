package com.homesweet.homesweetback.domain.settlement.batch.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.annotation.AfterStep;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.stereotype.Component;

/**
 * Step 내부에서 예외가 발생했을 때 실패 정보를 로깅하거나
 * 추가적인 오류 처리 로직을 실행하는 Listener
 * Step 실패 시 오류 메시지, 예외 내용을 기록
 * 오류 Slack/Webhook 알림 발송 가능
 * 실패 Step 재시작 요구 사항 분석
 * 특징
 * - 정산 생성/취소/집계 Step의 예외 관리를 중앙화 가능
 * - 운영 환경에서 장애 모니터링의 핵심 역할
 */
@Slf4j
@Component
public class SettlementStepFailListener implements StepExecutionListener {
    // step 실행 전
    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        log.info("Step 실행 시작: {}", stepExecution.getStepName());
    }

    // step 실행 후
    @AfterStep
    public ExitStatus afterStep(StepExecution stepExecution) {
        // 실패 여부 확인
        if (stepExecution.getStatus() == BatchStatus.FAILED) {
            if (!stepExecution.getFailureExceptions().isEmpty()) {
                log.error("=== Step 실패 예외 전체 출력 시작 ===");
                for (Throwable e : stepExecution.getFailureExceptions()) {
                    log.error("예외: {}", e.getMessage(), e);
                }
                log.error("=== Step 실패 예외 전체 출력 끝 ===");
            }
        }
        log.info("step 실행 종료: {}", stepExecution.getStepName());
        return stepExecution.getExitStatus();
    }
}
