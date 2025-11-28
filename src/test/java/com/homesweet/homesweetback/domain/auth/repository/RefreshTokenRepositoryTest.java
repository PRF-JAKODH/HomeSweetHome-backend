package com.homesweet.homesweetback.domain.auth.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.homesweet.homesweetback.domain.product.category.service.cache.CacheCategory;
import com.homesweet.homesweetback.domain.product.product.command.repository.mapper.ProductMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.homesweet.homesweetback.common.config.QueryDslConfig;
import com.homesweet.homesweetback.domain.product.category.repository.impl.ProductCategoryRepositoryImpl;
import com.homesweet.homesweetback.domain.product.category.repository.mapper.ProductCategoryMapper;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("RefreshTokenRepository 테스트")
@Import({
    QueryDslConfig.class,
    ProductCategoryRepositoryImpl.class,
    ProductCategoryMapper.class,
    ProductMapper.class,
    InMemoryRefreshTokenRepository.class
})
class RefreshTokenRepositoryTest {
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    @MockitoBean
    private CacheCategory cacheCategory;
    
    private static final String TEST_EMAIL = "test@test.com";
    private static final String TEST_REFRESH_TOKEN = "test-refresh-token-12345";
    private static final String ANOTHER_EMAIL = "another@test.com";
    private static final String ANOTHER_REFRESH_TOKEN = "another-refresh-token-67890";
    
    @BeforeEach
    void setUp() {
        // 각 테스트 전에 저장소 초기화를 위해 모든 데이터 삭제
        refreshTokenRepository.deleteByEmail(TEST_EMAIL);
        refreshTokenRepository.deleteByEmail(ANOTHER_EMAIL);
    }
    
    @Test
    @DisplayName("RefreshToken 저장 테스트_성공")
    void testSave_Success() {
        // given
        String email = TEST_EMAIL;
        String refreshToken = TEST_REFRESH_TOKEN;

        // when
        boolean result = refreshTokenRepository.save(email, refreshToken);

        // then
        assertThat(result).isTrue();
        String savedToken = refreshTokenRepository.findByEmail(email);
        assertThat(savedToken).isEqualTo(refreshToken);
    }
    
    @Test
    @DisplayName("RefreshToken 저장 후 조회 테스트_성공")
    void testFindByEmail_Success() {
        // given
        refreshTokenRepository.save(TEST_EMAIL, TEST_REFRESH_TOKEN);

        // when
        String foundToken = refreshTokenRepository.findByEmail(TEST_EMAIL);

        // then
        assertThat(foundToken).isNotNull();
        assertThat(foundToken).isEqualTo(TEST_REFRESH_TOKEN);
    }
    
    @Test
    @DisplayName("존재하지 않는 이메일로 RefreshToken 조회 테스트_실패")
    void testFindByEmail_NotFound() {
        // given
        String nonExistentEmail = "nonexistent@test.com";

        // when
        String foundToken = refreshTokenRepository.findByEmail(nonExistentEmail);

        // then
        assertThat(foundToken).isNull();
    }
    
    @Test
    @DisplayName("RefreshToken 삭제 테스트_성공")
    void testDeleteByEmail_Success() {
        // given
        refreshTokenRepository.save(TEST_EMAIL, TEST_REFRESH_TOKEN);
        assertThat(refreshTokenRepository.findByEmail(TEST_EMAIL)).isNotNull();

        // when
        refreshTokenRepository.deleteByEmail(TEST_EMAIL);

        // then
        String deletedToken = refreshTokenRepository.findByEmail(TEST_EMAIL);
        assertThat(deletedToken).isNull();
    }
    
    @Test
    @DisplayName("존재하지 않는 이메일로 RefreshToken 삭제 테스트_실패")
    void testDeleteByEmail_NotFound() {
        // given
        String nonExistentEmail = "nonexistent@test.com";
        assertThat(refreshTokenRepository.findByEmail(nonExistentEmail)).isNull();

        // when & then - 예외가 발생하지 않아야 함
        refreshTokenRepository.deleteByEmail(nonExistentEmail);
        assertThat(refreshTokenRepository.findByEmail(nonExistentEmail)).isNull();
    }
    
    @Test
    @DisplayName("RefreshToken 업데이트 테스트_성공")
    void testUpdateRefreshToken_Success() {
        // given
        refreshTokenRepository.save(TEST_EMAIL, TEST_REFRESH_TOKEN);
        String newRefreshToken = "new-refresh-token-99999";

        // when
        boolean result = refreshTokenRepository.save(TEST_EMAIL, newRefreshToken);

        // then
        assertThat(result).isTrue();
        String updatedToken = refreshTokenRepository.findByEmail(TEST_EMAIL);
        assertThat(updatedToken).isEqualTo(newRefreshToken);
        assertThat(updatedToken).isNotEqualTo(TEST_REFRESH_TOKEN);
    }
    
    @Test
    @DisplayName("여러 사용자의 RefreshToken 저장 및 조회 테스트_성공")
    void testMultipleUsers_Success() {
        // given
        refreshTokenRepository.save(TEST_EMAIL, TEST_REFRESH_TOKEN);
        refreshTokenRepository.save(ANOTHER_EMAIL, ANOTHER_REFRESH_TOKEN);

        // when
        String token1 = refreshTokenRepository.findByEmail(TEST_EMAIL);
        String token2 = refreshTokenRepository.findByEmail(ANOTHER_EMAIL);

        // then
        assertThat(token1).isEqualTo(TEST_REFRESH_TOKEN);
        assertThat(token2).isEqualTo(ANOTHER_REFRESH_TOKEN);
        assertThat(token1).isNotEqualTo(token2);
    }
    
    @Test
    @DisplayName("RefreshToken 저장 후 삭제 후 재저장 테스트_성공")
    void testSaveDeleteSave_Success() {
        // given
        refreshTokenRepository.save(TEST_EMAIL, TEST_REFRESH_TOKEN);
        refreshTokenRepository.deleteByEmail(TEST_EMAIL);
        String newRefreshToken = "new-refresh-token-after-delete";

        // when
        refreshTokenRepository.save(TEST_EMAIL, newRefreshToken);

        // then
        String savedToken = refreshTokenRepository.findByEmail(TEST_EMAIL);
        assertThat(savedToken).isEqualTo(newRefreshToken);
        assertThat(savedToken).isNotEqualTo(TEST_REFRESH_TOKEN);
    }
}
