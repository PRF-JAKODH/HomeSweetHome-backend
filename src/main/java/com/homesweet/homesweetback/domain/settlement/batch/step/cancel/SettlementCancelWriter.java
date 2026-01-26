package com.homesweet.homesweetback.domain.settlement.batch.step.cancel;

import com.homesweet.homesweetback.domain.settlement.entity.Settlement;
import com.homesweet.homesweetback.domain.settlement.repository.SettlementRepository;
import com.homesweet.homesweetback.domain.settlement.validation.SettlementValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Component
@StepScope
public class SettlementCancelWriter implements ItemWriter<Settlement> {
    private final SettlementRepository settlementRepository;
    private final SettlementValidator settlementValidator;

    @Override
    public void write(Chunk<? extends Settlement> chunk) {
        List<? extends Settlement> settlements = chunk.getItems();
        settlementValidator.validateNotEmpty(settlements);

        settlementRepository.saveAll(settlements);
        log.info("[정산 취소 writer] {}건 정산 취소 처리 완료", settlements.size());
    }
}
