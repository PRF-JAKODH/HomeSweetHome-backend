package com.homesweet.homesweetback.domain.settlement.batch.step.cancel;

import com.homesweet.homesweetback.common.exception.BusinessException;
import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.settlement.data.HelperData;
import com.homesweet.homesweetback.domain.settlement.entity.Settlement;
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
import org.springframework.batch.item.Chunk;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("정산 취소 writer 단위 테스트")
class SettlementCancelWriterTest {
    @Mock
    private SettlementRepository settlementRepository;
    @InjectMocks
    private SettlementCancelWriter settlementCancelWriter;
    @Mock
    private SettlementValidator settlementValidator;

    @BeforeEach
    void setUp() {
        settlementCancelWriter = new SettlementCancelWriter(settlementRepository, settlementValidator);
    }

    @Nested
    @DisplayName("성공 케이스")
    class Success {
        @Test
        @DisplayName("정산 취소 writer 정상 처리")
        void write_success() {
            // given
            Settlement s1 = HelperData.createSettlement(1L, HelperData.getOrder(HelperData.getUser()));
            Settlement s2 = HelperData.createSettlement(2L, HelperData.getOrder(HelperData.getUser()));
            List<Settlement> list = List.of(s1, s2);

            Chunk<Settlement> chunk = Chunk.of(s1, s2);

            // when
            settlementCancelWriter.write(chunk);

            // then
            verify(settlementValidator, times(1)).validateNotEmpty(list);
            verify(settlementRepository, times(1)).saveAll(list);
        }

        @Test
        @DisplayName("여러 정산값도 saveAll에 그대로 전달된다")
        void write_success_multiItems() {
            Settlement s1 = HelperData.createSettlement(1L, HelperData.getOrder(HelperData.getUser()));
            Settlement s2 = HelperData.createSettlement(2L, HelperData.getOrder(HelperData.getUser()));
            Settlement s3 = HelperData.createSettlement(3L, HelperData.getOrder(HelperData.getUser()));

            Chunk<Settlement> chunk = Chunk.of(s1, s2, s3);

            settlementCancelWriter.write(chunk);

            verify(settlementRepository).saveAll(List.of(s1, s2, s3));
        }
    }

    @Nested
    @DisplayName("실패 케이스")
    class Failure {
        @Test
        @DisplayName("빈 리스트이면 validateNotEmpty() 에서 예외 발생")
        void write_fail_emptyList() {
            Chunk<Settlement> chunk = Chunk.of();

            doThrow(new IllegalArgumentException("정산 취소 데이터가 비었습니다"))
                    .when(settlementValidator)
                    .validateNotEmpty(anyList());

            assertThatThrownBy(() -> settlementCancelWriter.write(chunk))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("정산 취소 데이터가 비었습니다");

            verify(settlementRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("Validator가 BusinessException을 발생시키면 그대로 전파")
        void write_fail_validatorError() {
            Settlement s1 = HelperData.createSettlement(1L, HelperData.getOrder(HelperData.getUser()));
            Chunk<Settlement> chunk = Chunk.of(s1);

            doThrow(new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND))
                    .when(settlementValidator)
                    .validateNotEmpty(anyList());

            assertThatThrownBy(() -> settlementCancelWriter.write(chunk))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ErrorCode.SETTLEMENT_NOT_FOUND.getMessage());

            verify(settlementRepository, never()).saveAll(anyList());
        }
    }
}