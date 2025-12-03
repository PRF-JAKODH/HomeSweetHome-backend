package com.homesweet.homesweetback.domain.auth.repository;

import com.homesweet.homesweetback.domain.auth.entity.OAuth2Provider;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.entity.UserRole;

import io.lettuce.core.dynamic.annotation.Param;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    /**
     * OAuth Provider와 Provider ID로 사용자 조회
     */
    Optional<User> findByProviderAndProviderId(OAuth2Provider provider, String providerId);

    List<User> findAllByRole(UserRole role);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.grade WHERE u.id IN :userIds")
    List<User> findAllByIdIn(@Param("userIds") List<Long> userIds);
}
