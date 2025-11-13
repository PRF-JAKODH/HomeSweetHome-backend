package com.homesweet.homesweetback.domain.auth.service;

import com.homesweet.homesweetback.domain.auth.dto.UpdateUserRequest;
import com.homesweet.homesweetback.domain.auth.dto.UpdateUserRoleRequest;
import com.homesweet.homesweetback.domain.auth.dto.UserResponse;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.grade.entity.Grade;
import com.homesweet.homesweetback.domain.grade.repository.GradeRepository;
import com.homesweet.homesweetback.domain.auth.repository.UserRepository;
import com.homesweet.homesweetback.common.util.PhoneNumberValidator;
import com.homesweet.homesweetback.common.exception.BusinessException;
import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.common.s3.ImageUploader;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 사용자 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ImageUploader imageUploader;
    private final GradeRepository gradeRepository;  
    /**
     * 사용자 정보 조회
     */
    @Transactional(readOnly = true)
    public UserResponse getUserInfo(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        
        log.debug("User info retrieved: {}", user.getEmail());
        return UserResponse.of(user);
    }

    @Transactional(readOnly = true)
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * 사용자 정보 수정
     */
    @Transactional
    public UserResponse updateUserInfo(Long userId, UpdateUserRequest request, Optional<MultipartFile> profileImage) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        
        if (profileImage.isPresent()) {
            try{
                imageUploader.delete(user.getProfileImageUrl());
            } catch (Exception e) {
                log.error("프로필 이미지 삭제 실패: userId={}", userId, e);
                throw new BusinessException(ErrorCode.FILE_STREAM_ERROR);
            }
            try{
                String uploadedUrl = imageUploader.upload(profileImage.get(), "user/profile/" + userId);
                user.setProfileImageUrl(uploadedUrl);
            } catch (Exception e) {
                log.error("프로필 이미지 업로드 실패: userId={}", userId, e);
                throw new BusinessException(ErrorCode.FILE_STREAM_ERROR);
            }
        }
        
        // 사용자 정보 업데이트
        if (request.name() != null && !request.name().trim().isEmpty()) {
            user.setName(request.name().trim());
        }
        
        if (request.phoneNumber() != null) {
            // 핸드폰 번호 검증
            if (!PhoneNumberValidator.isValid(request.phoneNumber())) {
                throw new BusinessException(ErrorCode.INVALID_PHONE_NUMBER_FORMAT);
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
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        
        userRepository.delete(user);
        log.info("User account deleted: {}", user.getEmail());
    }   

    /**
     * 사용자 역할 수정
     */
    @Transactional
    public UserResponse updateUserRole(Long userId, UpdateUserRoleRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        user.setRole(request.role());
        
        // 판매자 등급 랜덤 설정
        Integer gradeId = ThreadLocalRandom.current().nextInt(1, 6);
        Grade grade = gradeRepository.findById(gradeId)
            .orElseThrow(() -> new BusinessException(ErrorCode.GRADE_NOT_FOUND));
        user.setGrade(grade);
        
        return UserResponse.of(userRepository.save(user));
    }

    /**
     * 사용자의 등급 정보를 조회합니다.
     * 등급이 없는 경우 null을 반환합니다.
     */
    @Transactional(readOnly = true)
    public Optional<Grade> getUserGrade(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        
        return user.getGradeOptional();
    }

    /**
     * 사용자의 등급 이름을 조회합니다.
     * 등급이 없는 경우 "등급 없음"을 반환합니다.
     */
    @Transactional(readOnly = true)
    public String getUserGradeName(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        
        return user.getGradeName();
    }

    /**
     * 사용자의 수수료율을 조회합니다.
     * 등급이 없는 경우 0.0을 반환합니다.
     */
    @Transactional(readOnly = true)
    public BigDecimal getUserFeeRate(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        
        return user.getFeeRate();
    }

    /**
     * 사용자가 등급을 가지고 있는지 확인합니다.
     */
    @Transactional(readOnly = true)
    public boolean hasUserGrade(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        
        return user.hasGrade();
    }
}
