package com.homesweet.homesweetback.domain.settlement.util.saver;

import com.homesweet.homesweetback.domain.settlement.repository.DailySettlementRepository;
import com.homesweet.homesweetback.domain.settlement.repository.SettlementRepository;
import com.homesweet.homesweetback.domain.settlement.util.vo.DailyTotals;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("upsert ")
class DailySettlementSaverTest {

    @InjectMocks
    private DailySettlementSaver dailySettlementSaver;

    @Mock
    private DailySettlementRepository dailySettlementRepository;

    @Test
    @DisplayName("일별 집계에서 정상적으로 upsert 호출합니다.")
    void saveDaily() {
        // given
        Long userId = 1L;
        LocalDate date = LocalDate.of(2025, 11, 10);
        DailyTotals totals = DailyTotals.empty();

        // when
        dailySettlementSaver.saveDaily(userId, date, totals);

        // then
        verify(dailySettlementRepository, times(1))
                .upsertDaily(
                        eq(userId),
                        eq(date.atStartOfDay()),
                        eq(totals.getTotalSales()),
                        eq(totals.getTotalFee()),
                        eq(totals.getTotalVat()),
                        eq(totals.getTotalRefund()),
                        eq(totals.getTotalSettlement())
                );
    }

    @Test
    @DisplayName("[실패] Repository가 예외를 던지면 saveDaily도 예외를 전달한다")
    void saveDaily_Failure_RepositoryException() {
        // given
        Long userId = 1L;
        LocalDate date = LocalDate.of(2025, 11, 10);
        DailyTotals totals = DailyTotals.empty();

        doThrow(new RuntimeException("DB ERROR"))
                .when(dailySettlementRepository)
                .upsertDaily(anyLong(), any(), any(), any(), any(), any(), any());

        // when & then
        assertThatThrownBy(() -> dailySettlementSaver.saveDaily(userId, date, totals))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("DB ERROR");
    }
}