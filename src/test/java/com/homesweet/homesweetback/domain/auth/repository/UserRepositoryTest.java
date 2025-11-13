package com.homesweet.homesweetback.domain.auth.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.homesweet.homesweetback.common.config.QueryDslConfig;
import com.homesweet.homesweetback.domain.auth.entity.OAuth2Provider;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.entity.UserRole;
import com.homesweet.homesweetback.domain.product.category.repository.impl.ProductCategoryRepositoryImpl;
import com.homesweet.homesweetback.domain.product.category.repository.mapper.ProductCategoryMapper;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("UserRepository 테스트")
@Import({
    QueryDslConfig.class,
    ProductCategoryRepositoryImpl.class,
    ProductCategoryMapper.class
})
class UserRepositoryTest {
    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("UserRole.USER 권한을 가진 사용자 조회 테스트_성공")
    void testFindAllByRoleUser_Success() {
        // given
        User user = createTestUser(UserRole.USER);
        userRepository.save(user);

        // when
        List<User> users = userRepository.findAllByRole(UserRole.USER);

        // then
        assertThat(users).isNotEmpty();
        assertThat(users.size()).isEqualTo(1);
        assertThat(users.get(0).getRole()).isEqualTo(UserRole.USER);
        assertThat(users.get(0).getProvider()).isEqualTo(OAuth2Provider.GOOGLE);
        assertThat(users.get(0).getProviderId()).isEqualTo("123456789");
    }

    @Test
    @DisplayName("UserRole.SELLER 권한을 가진 사용자 조회 테스트_성공")
    void testFindAllByRoleSeller_Success() {
        // given
        User user = createTestUser(UserRole.SELLER);
        userRepository.save(user);

        // when
        List<User> users = userRepository.findAllByRole(UserRole.SELLER);

        // then
        assertThat(users).isNotEmpty();
        assertThat(users.size()).isEqualTo(1);
        assertThat(users.get(0).getRole()).isEqualTo(UserRole.SELLER);
        assertThat(users.get(0).getProvider()).isEqualTo(OAuth2Provider.GOOGLE);
        assertThat(users.get(0).getProviderId()).isEqualTo("123456789");
    }

    @Test
    @DisplayName("UserRole.USER 권한을 가진 사용자 조회 테스트_실패")
    void testFindAllByRoleUser_Fail() {
        // given
        User user = createTestUser(UserRole.SELLER);
        userRepository.save(user);

        // when
        List<User> users = userRepository.findAllByRole(UserRole.USER);

        // then
        assertThat(users).isEmpty();
    }

    @Test
    @DisplayName("UserRole.SELLER 권한을 가진 사용자 조회 테스트_실패")
    void testFindAllByRoleSeller_Fail() {
        // given
        User user = createTestUser(UserRole.USER);
        userRepository.save(user);

        // when
        List<User> users = userRepository.findAllByRole(UserRole.SELLER);

        // then
        assertThat(users).isEmpty();
    }

    @Test
    @DisplayName("Provider와 ProviderId로 사용자 조회 테스트_성공")
    void testFindByProviderAndProviderId_Success() {
        // given
        User user = createTestUser(UserRole.USER);
        userRepository.save(user);

        // when
        Optional<User> foundUser = userRepository.findByProviderAndProviderId(OAuth2Provider.GOOGLE, "123456789");

        // then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getRole()).isEqualTo(UserRole.USER);
        assertThat(foundUser.get().getProvider()).isEqualTo(OAuth2Provider.GOOGLE);
        assertThat(foundUser.get().getProviderId()).isEqualTo("123456789");
    }

    @Test
    @DisplayName("Provider와 ProviderId로 사용자 조회 테스트_실패")
    void testFindByProviderAndProviderId_Fail() {
        // given
        // when
        Optional<User> foundUser = userRepository.findByProviderAndProviderId(OAuth2Provider.GOOGLE, "987654321");

        // then
        assertThat(foundUser).isEmpty();
    }

    private User createTestUser(UserRole role) {
        return User.builder()
                .email("test@test.com")
                .name("test")
                .role(role)
                .provider(OAuth2Provider.GOOGLE)
                .providerId("123456789")
                .build();
    }
}
