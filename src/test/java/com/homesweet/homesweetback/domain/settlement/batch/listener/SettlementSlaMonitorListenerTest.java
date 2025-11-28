package com.homesweet.homesweetback.domain.settlement.batch.listener;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("sla 모니터링 listener")
class SettlementSlaMonitorListenerTest {
    SettlementSlaMonitorListener listener = new SettlementSlaMonitorListener();

    @Nested
    @DisplayName("성공 케이스")
    class Success {
        @Test
        @DisplayName("BeforeStep 실행 시 SLAStartTime 이 저장된다")
        void beforeStep_success() {
            StepExecution stepExecution = new StepExecution("dailyStep", null);

            listener.beforeStep(stepExecution);

            long startTime =
                    stepExecution.getExecutionContext().getLong("SLAStartTime");

            assertThat(startTime).isGreaterThan(0);
        }

        @Test
        @DisplayName("AfterStep 실행 시 SSE 초과하지 않으면 ExitStatus 변경 없이 그대로 반환")
        void afterStep_success_underSLA() {
            StepExecution stepExecution = new StepExecution("dailyStep", null);

            // beforeStep()으로 시작 시간 세팅
            listener.beforeStep(stepExecution);

            // 실행시간을 짧게 하기 위해 sleep 생략

            ExitStatus status = listener.afterStep(stepExecution);

            // ExitStatus 그대로 유지
            assertThat(status).isEqualTo(stepExecution.getExitStatus());
        }
    }

    @Nested
    @DisplayName("실패 케이스")
    class Failure {

        @Test
        @DisplayName("AfterStep 실행 시 SLA 초과하면 로그 경고 발생 (ExitStatus는 변경 없음)")
        void afterStep_fail_overSLA() throws Exception {
            StepExecution stepExecution = new StepExecution("dailyStep", null);

            // 강제로 SLAStartTime 을 과거로 설정 → SLA 초과 유도
            long pastTime = System.currentTimeMillis() - 15_000; // 15초 전
            stepExecution.getExecutionContext().putLong("SLAStartTime", pastTime);

            ExitStatus result = listener.afterStep(stepExecution);

            // ExitStatus는 변경되지 않음
            assertThat(result).isEqualTo(stepExecution.getExitStatus());
        }
    }
}