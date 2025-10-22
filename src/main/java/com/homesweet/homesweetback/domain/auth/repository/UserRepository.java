package com.homesweet.homesweetback.domain.auth.repository;

import com.homesweet.homesweetback.domain.auth.entity.OAuth2Provider;
import com.homesweet.homesweetback.domain.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByEmail(String email);
    
    boolean existsByEmail(String email);
    
    /**
     * OAuth Provider와 Provider ID로 사용자 조회
     */
    Optional<User> findByProviderAndProviderId(OAuth2Provider provider, String providerId);
    
    /**
     * OAuth Provider와 Provider ID로 사용자 존재 여부 확인
     */
    boolean existsByProviderAndProviderId(OAuth2Provider provider, String providerId);
    
    /**
     * 이메일과 Provider로 사용자 조회
     */
    Optional<User> findByEmailAndProvider(String email, OAuth2Provider provider);
}
