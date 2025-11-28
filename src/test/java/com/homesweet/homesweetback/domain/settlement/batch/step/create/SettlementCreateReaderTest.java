package com.homesweet.homesweetback.domain.settlement.batch.step.create;

import com.homesweet.homesweetback.domain.order.entity.Order;
import com.homesweet.homesweetback.domain.order.entity.OrderStatus;
import com.homesweet.homesweetback.domain.settlement.data.HelperData;
import com.homesweet.homesweetback.domain.settlement.repository.SettlementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@SpringBatchTest
@ExtendWith(MockitoExtension.class)
@DisplayName("정산 생성 reader 단위 테스트")
class SettlementCreateReaderTest {
    @Mock
    private SettlementRepository settlementRepository;
    @InjectMocks
    private SettlementCreateReader reader;

    private ExecutionContext executionContext;

    @BeforeEach
    void setup() {
        executionContext = new ExecutionContext();
    }

    @BeforeEach
    void resetReader() {
        reader = new SettlementCreateReader(settlementRepository);
    }

    @Nested
    @DisplayName("성공 케이스")
    class Success {
        @Test
        @DisplayName("첫 read()에서 cutoff 적용 후 Repository 1회만 호출된다")
        void read_success_repoCalledOnce() throws Exception {

            // given
            String cutoffStr = "2025-01-01T00:00:00";
            LocalDateTime cutoff = LocalDateTime.parse(cutoffStr);

            setJobParameter("cutoff", cutoffStr);

            Order o1 = HelperData.getOrder(HelperData.getUser());
            Order o2 = HelperData.getOrder(HelperData.getUser());

            given(settlementRepository.findUnSettlementOrders(OrderStatus.COMPLETED, cutoff))
                    .willReturn(List.of(o1, o2));

            // when
            Order r1 = reader.read();
            Order r2 = reader.read();
            Order r3 = reader.read(); // null

            // then
            assertThat(r1).isEqualTo(o1);
            assertThat(r2).isEqualTo(o2);
            assertThat(r3).isNull();
            verify(settlementRepository, times(1))
                    .findUnSettlementOrders(OrderStatus.COMPLETED, cutoff);
        }

        @Test
        @DisplayName("빈 리스트를 반환하면 즉시 null 반환한다")
        void read_success_emptyList_returnsNull() throws Exception {

            String cutoffStr = "2025-01-01T00:00:00";
            LocalDateTime cutoff = LocalDateTime.parse(cutoffStr);

            setJobParameter("cutoff", cutoffStr);

            given(settlementRepository.findUnSettlementOrders(OrderStatus.COMPLETED, cutoff))
                    .willReturn(List.of());

            Order r1 = reader.read();

            assertThat(r1).isNull();
            verify(settlementRepository, times(1))
                    .findUnSettlementOrders(OrderStatus.COMPLETED, cutoff);
        }

        @Test
        @DisplayName("여러 개의 주문이 있으면 순서대로 반환 후 null")
        void read_success_multi() throws Exception {

            String cutoffStr = "2025-01-01T00:00:00";
            LocalDateTime cutoff = LocalDateTime.parse(cutoffStr);

            setJobParameter("cutoff", cutoffStr);

            Order o1 = HelperData.getOrder(HelperData.getUser());
            Order o2 = HelperData.getOrder(HelperData.getUser());
            Order o3 = HelperData.getOrder(HelperData.getUser());

            given(settlementRepository.findUnSettlementOrders(OrderStatus.COMPLETED, cutoff))
                    .willReturn(List.of(o1, o2, o3));

            assertThat(reader.read()).isEqualTo(o1);
            assertThat(reader.read()).isEqualTo(o2);
            assertThat(reader.read()).isEqualTo(o3);
            assertThat(reader.read()).isNull();
        }
    }

    @Nested
    @DisplayName("실패 케이스")
    class Fail {
        @Test
        @DisplayName("cutoffString이 null이면 LocalDateTime.parse()에서 NPE 발생")
        void read_fail_cutoffNull() {
            setJobParameter("cutoff", null);

            assertThatThrownBy(() -> reader.read())
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("cutoffString 형식이 잘못되면 DateTimeParseException 발생")
        void read_fail_invalidCutoffFormat() {
            setJobParameter("cutoff", "invalid-date-format");

            assertThatThrownBy(() -> reader.read())
                    .isInstanceOf(DateTimeParseException.class);
        }

        @Test
        @DisplayName("Repository 조회 중 에러 발생 시 예외")
        void read_fail_repoThrows() {
            // given
            String cutoffStr = "2025-01-01T00:00:00";
            LocalDateTime cutoff = LocalDateTime.parse(cutoffStr);

            // ReflectionTestUtils 로 cutoffString 을 명확히 reader에 주입
            ReflectionTestUtils.setField(reader, "cutoffString", cutoffStr);

            // settlementRepository Mock이 Reader에 정확히 삽입되었는지 1회 검증
            assertThat(reader).isNotNull();
            assertThat(settlementRepository).isNotNull();

            // stubbing (Reader가 사용하는 settlementRepository mock에 대한 stubbing)
            given(settlementRepository.findUnSettlementOrders(OrderStatus.COMPLETED, cutoff))
                    .willThrow(new RuntimeException("DB error"));

            // when & then
            assertThatThrownBy(() -> reader.read())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("DB error");
        }

    }

    private void setJobParameter(String key, String value) {
        ReflectionTestUtils.setField(reader, "cutoffString", value);
    }

}