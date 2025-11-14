package com.homesweet.homesweetback.domain.settlement.util;

import com.homesweet.homesweetback.domain.settlement.repository.SettlementRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;

import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("집계시 정산 상태 변경")
public class SettlementStatusUpdaterTest {
    @Mock
    private SettlementRepository settlementRepository;

    @InjectMocks
    private SettlementStatusUpdater settlementStatusUpdater;

    @Test
    @DisplayName("[성공] markDailyCompleted는 메소드 호출")
    void markDailyCompleted() {
        // given
        Long userId = 1L;
        LocalDateTime start = LocalDateTime.of(2025, 11, 10, 0, 0);
        LocalDateTime end = LocalDateTime.of(2025, 11, 11, 0, 0);

        // when
        settlementStatusUpdater.markDailyCompleted(userId, start, end);
        then(settlementRepository)
                .should(times(1))
                .markCompletedInRange(userId, start, end);
    }
}
