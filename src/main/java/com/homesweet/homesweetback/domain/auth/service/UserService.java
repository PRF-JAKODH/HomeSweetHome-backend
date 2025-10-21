package com.homesweet.homesweetback.domain.auth.service;

import com.homesweet.homesweetback.domain.auth.dto.UpdateUserRequest;
import com.homesweet.homesweetback.domain.auth.dto.UpdateUserRoleRequest;
import com.homesweet.homesweetback.domain.auth.dto.UserResponse;
import com.homesweet.homesweetback.domain.auth.entity.User;
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
    public UserResponse updateUserInfo(Long userId, UpdateUserRequest request) {
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
        
        if (request.address() != null) {
            user.setAddress(request.address());
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
     * 사용자 역할 수정
     */
    @Transactional
    public UserResponse updateUserRole(Long userId, UpdateUserRoleRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        user.setRole(request.role());
        return UserResponse.of(userRepository.save(user));
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
