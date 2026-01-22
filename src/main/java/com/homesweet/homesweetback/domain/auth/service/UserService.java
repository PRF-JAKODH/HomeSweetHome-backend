package com.homesweet.homesweetback.domain.auth.service;

import com.homesweet.homesweetback.domain.auth.dto.UpdateUserRequest;
import com.homesweet.homesweetback.domain.auth.dto.UpdateUserRoleRequest;
import com.homesweet.homesweetback.domain.auth.dto.UserResponse;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.repository.UserRepository;
import com.homesweet.homesweetback.common.util.PhoneNumberValidator;
import com.homesweet.homesweetback.common.exception.BusinessException;
import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.common.s3.ImageUploader;

import java.util.Optional;

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
        
        return UserResponse.of(userRepository.save(user));
    }
}
