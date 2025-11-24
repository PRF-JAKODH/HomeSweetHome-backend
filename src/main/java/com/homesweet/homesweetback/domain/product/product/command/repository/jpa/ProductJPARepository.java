package com.homesweet.homesweetback.domain.product.product.command.repository.jpa;

import com.homesweet.homesweetback.domain.product.product.command.repository.jpa.entity.ProductEntity;
import com.homesweet.homesweetback.domain.product.product.command.repository.jpa.querydsl.CustomProductRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * 제품 JPA 레포
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 21.
 */
public interface ProductJPARepository extends JpaRepository<ProductEntity, Long>, CustomProductRepository {

    boolean existsBySellerIdAndName(Long sellerId, String name);

    Optional<ProductEntity> findByIdAndSellerId(Long productId, Long sellerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM ProductEntity p WHERE p.id = :id")
    Optional<ProductEntity> findByIdWithPessimisticLock(@Param("id")Long id);
}
