package com.homesweet.homesweetback.domain.product.category.repository.jpa;

import com.homesweet.homesweetback.domain.product.category.repository.jpa.entity.ProductCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * 제품 카테고리 JPA 레포
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 21.
 */
public interface ProductCategoryJPARepository extends JpaRepository<ProductCategoryEntity, Long> {

    Optional<ProductCategoryEntity> findByName(String name);

    List<ProductCategoryEntity> findByParentId(Long parentId);

    List<ProductCategoryEntity> findByParentIdIsNull();

    @Query(
            value = """
            WITH RECURSIVE category_hierarchy AS (
                SELECT category_id
                FROM product_category
                WHERE category_id = :categoryId
                UNION ALL
                SELECT c.category_id
                FROM product_category c
                INNER JOIN category_hierarchy ch
                ON c.parent_id = ch.category_id
            )
            SELECT category_id FROM category_hierarchy
            """,
            nativeQuery = true
    )
    List<Long> findAllSubCategoryIds(Long categoryId);
}
