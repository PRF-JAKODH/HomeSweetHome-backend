package com.homesweet.homesweetback.domain.auth.service;

import com.homesweet.homesweetback.domain.auth.dto.UpdateUserRequest;
import com.homesweet.homesweetback.domain.auth.dto.UserResponse;
import com.homesweet.homesweetback.domain.auth.dto.SignupRequest;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.entity.UserRole;
import com.homesweet.homesweetback.domain.auth.entity.Grade;
import com.homesweet.homesweetback.domain.auth.repository.UserRepository;
import com.homesweet.homesweetback.common.util.PhoneNumberValidator;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    /**
     * 사용자 정보 조회
     */
    public UserResponse getUserInfo(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        
        log.debug("User info retrieved: {}", user.getEmail());
        return UserResponse.of(user);
    }

    /**
     * 사용자 정보 수정
     */
    @Transactional
    public UserResponse updateUser(Long userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        
        // 사용자 정보 업데이트
        if (request.name() != null && !request.name().trim().isEmpty()) {
            user.setName(request.name().trim());
        }
        
        if (request.profileImageUrl() != null) {
            user.setProfileImageUrl(request.profileImageUrl());
        }
        
        if (request.phoneNumber() != null) {
            // 핸드폰 번호 검증
            if (!PhoneNumberValidator.isValid(request.phoneNumber())) {
                throw new IllegalArgumentException("올바른 핸드폰 번호 형식이 아닙니다");
            }
            user.setPhoneNumber(PhoneNumberValidator.format(request.phoneNumber()));
        }
        
        if (request.birthDate() != null) {
            user.setBirthDate(request.birthDate());
        }
        
        User updatedUser = userRepository.save(user);
        log.info("User info updated: {}", updatedUser.getEmail());
        
        return UserResponse.of(updatedUser);
    }

    /**
     * 사용자 계정 삭제 (탈퇴)
     */
    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId) 
            .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        
        userRepository.delete(user);
        log.info("User account deleted: {}", user.getEmail());
    }   

    /**
     * OAuth2 사용자 정보 저장/업데이트
     * 회원 가입시 사용
     */
    @Transactional
    public User saveOrUpdateOAuth2User(User user) {
        return userRepository.findByProviderAndProviderId(user.getProvider(), user.getProviderId())
            .map(existingUser -> {
                // 기존 사용자 정보 업데이트
                existingUser.setEmail(user.getEmail());
                existingUser.setName(user.getName());
                existingUser.setProfileImageUrl(user.getProfileImageUrl());
                // Grade는 Optional 패턴으로 안전하게 처리
                user.getGradeOptional().ifPresent(existingUser::setGrade);
                // Role은 기존 사용자의 것을 유지 (변경하지 않음)
                
                log.info("OAuth2 user updated: {}", existingUser.getEmail());
                return userRepository.save(existingUser);
            })
            .orElseGet(() -> {
                // 새 사용자 저장 - Role이 설정되지 않은 경우 기본값 USER로 설정
                if (user.getRole() == null) {
                    user.setRole(UserRole.USER);
                }
                User savedUser = userRepository.save(user);
                log.info("OAuth2 user created: {}", savedUser.getEmail());
                return savedUser;
            });
    }

    /**
     * 회원가입 완료 (핸드폰 번호, 생일, 역할 설정)
     */
    @Transactional
    public UserResponse completeSignup(Long userId, SignupRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        
        // 핸드폰 번호 검증
        if (!PhoneNumberValidator.isValid(request.phoneNumber())) {
            throw new IllegalArgumentException("올바른 핸드폰 번호 형식이 아닙니다");
        }
        
        // 생일 검증 (미래 날짜 불가)
        if (request.birthDate().isAfter(java.time.LocalDate.now())) {
            throw new IllegalArgumentException("생일은 미래 날짜가 될 수 없습니다");
        }
        
        // 사용자 정보 업데이트
        user.setPhoneNumber(PhoneNumberValidator.format(request.phoneNumber()));
        user.setBirthDate(request.birthDate());
        
        User updatedUser = userRepository.save(user);
        log.info("User signup completed: {} (phone: {}, birthDate: {})", 
                updatedUser.getEmail(), updatedUser.getPhoneNumber(), 
                updatedUser.getBirthDate());
        
        return UserResponse.of(updatedUser);
    }

    /**
     * 사용자의 등급 정보를 조회합니다.
     * 등급이 없는 경우 null을 반환합니다.
     */
    public Optional<Grade> getUserGrade(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        
        return user.getGradeOptional();
    }

    /**
     * 사용자의 등급 이름을 조회합니다.
     * 등급이 없는 경우 "등급 없음"을 반환합니다.
     */
    public String getUserGradeName(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        
        return user.getGradeName();
    }

    /**
     * 사용자의 수수료율을 조회합니다.
     * 등급이 없는 경우 0.0을 반환합니다.
     */
    public Double getUserFeeRate(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        
        return user.getFeeRate();
    }

    /**
     * 사용자가 등급을 가지고 있는지 확인합니다.
     */
    public boolean hasUserGrade(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        
        return user.hasGrade();
    }
}
