package com.homesweet.homesweetback.domain.product.product.repository.jpa.querydsl;

// --- Imports ---
import com.homesweet.homesweetback.common.config.QueryDslConfig;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.repository.UserRepository;
import com.homesweet.homesweetback.domain.product.category.repository.impl.ProductCategoryRepositoryImpl;
import com.homesweet.homesweetback.domain.product.category.repository.jpa.entity.ProductCategoryEntity;
import com.homesweet.homesweetback.domain.product.category.repository.jpa.ProductCategoryJPARepository;
import com.homesweet.homesweetback.domain.product.category.repository.mapper.ProductCategoryMapper;
import com.homesweet.homesweetback.domain.product.product.domain.ProductStatus;
import com.homesweet.homesweetback.domain.product.product.repository.jpa.ProductJPARepository;
import com.homesweet.homesweetback.domain.product.product.repository.jpa.SkuJPARepository;
import com.homesweet.homesweetback.domain.product.product.repository.jpa.entity.ProductEntity;
import com.homesweet.homesweetback.domain.product.product.repository.jpa.entity.SkuEntity;
import com.homesweet.homesweetback.domain.auth.entity.OAuth2Provider;
import com.homesweet.homesweetback.domain.auth.entity.UserRole;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test") // H2 MySQL 호환 모드 활성화
@Import({QueryDslConfig.class,
        ProductCategoryRepositoryImpl.class,
        ProductCategoryMapper.class})
class SkuJPARepositoryTest {

    @Autowired
    private SkuJPARepository skuJPARepository; // 👈 테스트 대상

    // --- GIVEN (setUp)에 필요한 의존성 ---
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProductCategoryJPARepository productCategoryJPARepository;
    @Autowired
    private ProductJPARepository productJPARepository;

    private Long savedSkuId; // 👈 테스트에서 사용할 SKU ID

    @BeforeEach
    void setUp() {
        // (OrderRepositoryTest와 동일한 GIVEN 데이터 설정)
        User user = User.builder()
                .email("testuser@example.com")
                .name("테스트유저")
                .provider(OAuth2Provider.GOOGLE)
                .role(UserRole.USER)
                .build();
        User savedUser = userRepository.save(user);

        ProductCategoryEntity category = ProductCategoryEntity.builder()
                .name("테스트 카테고리")
                .depth(0)
                .build();
        ProductCategoryEntity savedCategoryEntity = productCategoryJPARepository.save(category);

        ProductEntity product = ProductEntity.builder()
                .name("테스트 상품")
                .basePrice(10000)
                .discountRate(BigDecimal.ZERO)
                .shippingPrice(3000)
                .brand("테스트브랜드")
                .category(savedCategoryEntity)
                .seller(savedUser)
                .imageUrl("http://example.com/image.jpg")
                .status(ProductStatus.ON_SALE)
                .build();
        ProductEntity savedProduct = productJPARepository.save(product);

        SkuEntity sku = SkuEntity.builder()
                .product(savedProduct)
                .priceAdjustment(0)
                .stockQuantity(100L)
                .build();

        // [핵심] 테스트에서 사용할 SKU를 미리 저장하고 ID를 저장해 둠
        SkuEntity savedSku = skuJPARepository.save(sku);
        savedSkuId = savedSku.getId();
    }

    @Test
    @DisplayName("findByIdWithPessimisticLock 쿼리가 정상적으로 SKU를 조회한다.")
    void findByIdWithPessimisticLock_Success() {

        // --- GIVEN ---
        // (@BeforeEach에서 이미 'savedSku'를 H2 DB에 저장함)

        // --- WHEN ---
        // [핵심] @Lock 및 @Query 어노테이션이 붙은 쿼리를 "진짜" 호출
        Optional<SkuEntity> foundSkuOptional = skuJPARepository.findByIdWithPessimisticLock(savedSkuId);

        // --- THEN ---
        // 1. 조회가 성공했는지?
        assertThat(foundSkuOptional).isPresent();

        // 2. 조회된 SKU ID가 GIVEN에서 저장한 ID와 일치하는지?
        assertThat(foundSkuOptional.get().getId()).isEqualTo(savedSkuId);

        // 3. (부가) 재고 수량이 일치하는지?
        assertThat(foundSkuOptional.get().getStockQuantity()).isEqualTo(100L);
    }
}