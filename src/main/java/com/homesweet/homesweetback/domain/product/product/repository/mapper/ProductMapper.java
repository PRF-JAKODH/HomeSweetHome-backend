package com.homesweet.homesweetback.domain.product.product.repository.mapper;

import com.homesweet.homesweetback.domain.product.category.repository.mapper.ProductCategoryMapper;
import com.homesweet.homesweetback.domain.product.product.domain.Product;
import com.homesweet.homesweetback.domain.product.product.repository.jpa.entity.ProductEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 제품 도메인 <-> 엔티티 변환 클래스
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 22.
 */
@Component
@RequiredArgsConstructor
public class ProductMapper {

    private final ProductCategoryMapper categoryMapper;

    // Entity -> Domain
    public Product toDomain(ProductEntity entity) {

    }

    // Domain -> Entity
    public ProductEntity toEntity(Product domain) {

    }
}
