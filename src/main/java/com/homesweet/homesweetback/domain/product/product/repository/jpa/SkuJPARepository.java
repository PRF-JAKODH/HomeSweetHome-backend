package com.homesweet.homesweetback.domain.product.product.repository.jpa;

import com.homesweet.homesweetback.domain.product.product.repository.jpa.entity.SkuEntity;
import jakarta.persistence.LockModeType; // ★ LockModeType import
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock; // ★ Lock import
import org.springframework.data.jpa.repository.Query; // ★ Query import

import java.util.Optional;

public interface SkuJPARepository extends JpaRepository<SkuEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SkuEntity s WHERE s.id = :id")
    Optional<SkuEntity> findByIdWithPessimisticLock(Long id);
}