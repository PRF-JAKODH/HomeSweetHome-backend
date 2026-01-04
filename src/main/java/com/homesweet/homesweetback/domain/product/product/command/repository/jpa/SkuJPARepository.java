package com.homesweet.homesweetback.domain.product.product.command.repository.jpa;

import com.homesweet.homesweetback.domain.product.product.command.repository.jpa.entity.SkuEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SkuJPARepository extends JpaRepository<SkuEntity, Long> {
    @Query("SELECT s FROM SkuEntity s JOIN FETCH s.product WHERE s.id IN :ids")
    List<SkuEntity> findAllByIdWithProduct(@Param("ids") List<Long> ids);
}