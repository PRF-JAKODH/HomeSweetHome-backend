package com.homesweet.homesweetback.domain.settlement.batch.step.cancel;

import com.homesweet.homesweetback.domain.order.entity.DeliveryStatus;
import com.homesweet.homesweetback.domain.order.entity.Order;
import com.homesweet.homesweetback.domain.settlement.data.HelperData;
import com.homesweet.homesweetback.domain.settlement.repository.SettlementRepository;
import com.homesweet.homesweetback.domain.settlement.validation.SettlementValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("정산 취소 reader 단위 테스트")
class SettlementCancelReaderTest {

    @Mock
    private SettlementRepository settlementRepository;

    @Mock
    private SettlementValidator settlementValidator;

    @InjectMocks
    private SettlementCancelReader settlementCancelReader;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(settlementCancelReader, "cutoffString", "2025-01-01T00:00");
    }

    @Nested
    @DisplayName("성공 케이스")
    class Success {
        @Test
        @DisplayName("첫 read() 시 DB 조회되고 Order 반환")
        void read_success_firstCall_returnsOrder() {

            Order o1 = HelperData.getOrder(HelperData.getUser());

            LocalDateTime cutoff = LocalDateTime.parse("2025-01-01T00:00");

            given(settlementRepository.findCancelSettlement(DeliveryStatus.CANCELLED, cutoff))
                    .willReturn(List.of(o1));

            // when
            Order result = settlementCancelReader.read();

            // then
            assertThat(result).isEqualTo(o1);
            verify(settlementRepository, times(1))
                    .findCancelSettlement(DeliveryStatus.CANCELLED, cutoff);
        }

        @Test
        @DisplayName("두 번째 read() 시 repository 재조회 없이 null 반환")
        void read_success_secondCall_noDbCall() {

            Order o1 = HelperData.getOrder(HelperData.getUser());
            LocalDateTime cutoff = LocalDateTime.parse("2025-01-01T00:00");

            given(settlementRepository.findCancelSettlement(DeliveryStatus.CANCELLED, cutoff))
                    .willReturn(List.of(o1));

            // 첫 read() → DB 조회됨
            settlementCancelReader.read();
            // 두 번째 read() → DB 조회 없어야 함
            Order result2 = settlementCancelReader.read();

            assertThat(result2).isNull();

            // DB 조회는 반드시 1번만 호출
            verify(settlementRepository, times(1))
                    .findCancelSettlement(DeliveryStatus.CANCELLED, cutoff);
        }

        @Test
        @DisplayName("조회 결과가 empty 리스트면 read()는 null을 반환")
        void read_success_empty_returnsNull() {

            LocalDateTime cutoff = LocalDateTime.parse("2025-01-01T00:00");

            given(settlementRepository.findCancelSettlement(DeliveryStatus.CANCELLED, cutoff))
                    .willReturn(List.of());

            Order result = settlementCancelReader.read();

            assertThat(result).isNull();

            verify(settlementRepository, times(1))
                    .findCancelSettlement(DeliveryStatus.CANCELLED, cutoff);
        }
    }

    @Nested
    @DisplayName("실패 케이스")
    class Failure {
        @Test
        @DisplayName("Repository 에러 발생시 예외 전달")
        void read_fail_repoThrows() {

            LocalDateTime cutoff = LocalDateTime.parse("2025-01-01T00:00");

            given(settlementRepository.findCancelSettlement(DeliveryStatus.CANCELLED, cutoff))
                    .willThrow(new RuntimeException("DB error"));

            assertThatThrownBy(() -> settlementCancelReader.read())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("DB error");

            verify(settlementRepository, times(1))
                    .findCancelSettlement(DeliveryStatus.CANCELLED, cutoff);
        }
    }
}