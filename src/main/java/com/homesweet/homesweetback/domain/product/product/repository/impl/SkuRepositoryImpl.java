package com.homesweet.homesweetback.domain.product.product.repository.impl;

import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.product.product.domain.Sku;
import com.homesweet.homesweetback.domain.product.product.domain.exception.ProductException;
import com.homesweet.homesweetback.domain.product.product.repository.SkuRepository;
import com.homesweet.homesweetback.domain.product.product.repository.jpa.SkuJPARepository;
import com.homesweet.homesweetback.domain.product.product.repository.jpa.entity.SkuEntity;
import com.homesweet.homesweetback.domain.product.product.repository.mapper.SkuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 제품 옵션 레포 구현체
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 24.
 */
@Repository
@RequiredArgsConstructor
public class SkuRepositoryImpl implements SkuRepository {

    private final SkuJPARepository jpaRepository;
    private final SkuMapper mapper;

    @Override
    public Optional<Sku> findById(Long id) {
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsById(Long id) {
        return jpaRepository.existsById(id);
    }

    @Override
    public void updateSku(Long skuId, Long stockQuantity, Integer priceAdjustment) {
        SkuEntity entity = jpaRepository.findById(skuId)
                .orElseThrow(() -> new ProductException(ErrorCode.SKU_NOT_FOUND_ERROR));

        entity.updateStock(stockQuantity, priceAdjustment);
    }
}
