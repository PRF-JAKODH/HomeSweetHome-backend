package com.homesweet.homesweetback.domain.product.cart.repository.jpa;

import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.product.cart.repository.jpa.entity.CartEntity;
import com.homesweet.homesweetback.domain.product.cart.repository.jpa.querydsl.CustomCartRepository;
import com.homesweet.homesweetback.domain.product.product.command.repository.jpa.entity.SkuEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 장바구니 JPA 레포
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 24.
 */
public interface CartJPARepository extends JpaRepository<CartEntity, Long>, CustomCartRepository {
    List<CartEntity> sku(SkuEntity sku);

    Optional<CartEntity> findByUserIdAndSkuId(Long userId, Long skuId);

    boolean existsByIdAndUserId(Long cartId, Long userId);

    void deleteById(Long cartId);

    List<CartEntity> user(User user);

    void deleteAllByUserIdAndIdIn(Long userId, List<Long> cartIds);

    int countByUser_Id(Long userId);

    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM carts WHERE user_id = :userId AND sku_id IN (:skuIds)", nativeQuery = true)
    void deleteCartItemNative(@Param("userId") Long userId, @Param("skuIds") List<Long> skuIds); // 장바구니에서 구매가 완료된 SKU 목록 삭제 - 안채호
}
