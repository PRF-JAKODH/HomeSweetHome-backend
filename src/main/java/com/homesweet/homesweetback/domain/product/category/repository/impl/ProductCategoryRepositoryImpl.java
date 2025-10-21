package com.homesweet.homesweetback.domain.product.category.repository.impl;

import com.homesweet.homesweetback.domain.product.category.domain.ProductCategory;
import com.homesweet.homesweetback.domain.product.category.repository.ProductCategoryRepository;
import com.homesweet.homesweetback.domain.product.category.repository.jpa.ProductCategoryJPARepository;
import com.homesweet.homesweetback.domain.product.category.repository.jpa.entity.ProductCategoryEntity;
import com.homesweet.homesweetback.domain.product.category.repository.mapper.ProductCategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 제품 카테고리 레포 구현 코드 -> JPA 사용
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 21.
 */
@Repository
@RequiredArgsConstructor
public class ProductCategoryRepositoryImpl implements ProductCategoryRepository {

    private final ProductCategoryJPARepository jpaRepository;
    private final ProductCategoryMapper mapper;

    @Override
    public ProductCategory save(ProductCategory productCategory) {
        ProductCategoryEntity entity = jpaRepository.save(mapper.toEntity(productCategory));
        return mapper.toDomain(entity);
    }

    @Override
    public Optional<ProductCategory> findById(Long id) {
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<ProductCategory> findByName(String name) {
        return jpaRepository.findByName(name)
                .map(mapper::toDomain);
    }

    @Override
    public List<ProductCategory> findByParentId(Long parentId) {
        return jpaRepository.findByParentId(parentId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<ProductCategory> findTopLevelCategories() {
        return jpaRepository.findByParentIdIsNull().stream()
                .map(mapper::toDomain)
                .toList();
    }
}
