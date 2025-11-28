package com.homesweet.homesweetback.domain.settlement.batch.listener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.SkipListener;
import org.springframework.stereotype.Component;
/**
 * Chunk 기반 Step에서 Skip 발생 시 호출되는 Listener
 * Reader/Processor/Writer 단계에서 Skip 발생 시 로그 기록
 * Skip 된 데이터 저장 또는 관리자 알림
 * 대량 배치 환경에서 유효하지 않은 데이터 추적
 * 특징
 * - Chunk Step 전용 Listener
 * - 정산 배치에서 잘못된 주문/취소 데이터 감지에 필수
 */
@Slf4j
@Component
public class SettlementSkipListener implements SkipListener<Object, Object> {
    @Override
    public void onSkipInRead(Throwable throwable) {
        log.error("[SKIP] [READ] 정산 배치 읽기 실패 - error: {}", throwable.getMessage());
    }
    @Override
    public void onSkipInProcess(Object o, Throwable throwable) {
        log.error("[SKIP] [PROCESS] 정산 처리 실패 - object: {} - error: {}", safe(o), throwable.getMessage());
    }
    @Override
    public void onSkipInWrite(Object o, Throwable throwable) {
        log.error("[SKIP] [WRITE] 정산 쓰기 실패 - object: {} - error: {}", safe(o),throwable.getMessage());
    }
    private String safe(Object o) {
        return (o != null) ? o.toString() : "null";
    }
}
