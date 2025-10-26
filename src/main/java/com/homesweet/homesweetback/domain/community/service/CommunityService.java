package com.homesweet.homesweetback.domain.community.service;

import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.repository.UserRepository;
import com.homesweet.homesweetback.domain.community.dto.CommunityCreateRequest;
import com.homesweet.homesweetback.domain.community.dto.CommunityResponse;
import com.homesweet.homesweetback.domain.community.dto.exception.CommunityException;
import com.homesweet.homesweetback.domain.community.entity.CommunityImageEntity;
import com.homesweet.homesweetback.domain.community.entity.CommunityPostEntity;
import com.homesweet.homesweetback.domain.community.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

/**
 * Community 서비스
 *
 * @author ohhalim777@gmail.com
 * @date 25. 10. 21.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommunityService {

    private final CommunityPostRepository postRepository;
//    private final CommunityCommentRepository commentRepository;
    private final CommunityImageRepository imageRepository;
//    private final CommunityPostLikeRepository likeRepository;
//    private final CommunityCommentLikeRepository commentLikeRepository;
    private final UserRepository userRepository;
    private final CommunityImageUploader imageUploader;

    /**
     * 게시글 작성
     */
    @Transactional
    public CommunityResponse createPost(List<MultipartFile> images, CommunityCreateRequest request, Long userId) {
        // User 조회
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new CommunityException(ErrorCode.USER_NOT_FOUND));

        CommunityPostEntity savedPost = postRepository.save(
                CommunityPostEntity.builder()
                .author(author)
                .title(request.title())
                .content(request.content())
                .build()
        );

        // 이미지 업로드 및 저장
        List<String> imageUrls = new ArrayList<>();
        if (images != null && !images.isEmpty()) {
            imageUrls = imageUploader.uploadCommunityImages(images);

            for (int i = 0; i < imageUrls.size(); i++) {
                imageRepository.save(
                        CommunityImageEntity.builder()
                                .post(savedPost)
                                .imageUrl(imageUrls.get(i))
                                .imageOrder(i)
                                .build()
                );
            }
        }

        return CommunityResponse.from(savedPost, imageUrls);
    }

    /**
     * 게시글 단건 조회
     */
    public CommunityResponse getPost(Long postId) {
        CommunityPostEntity post = postRepository.findByPostIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new EntityNotFoundException("게시글을 찾을 수 없습니다."));

        // 이미지 조회
        List<String> imageUrls = imageRepository.findByPostOrderByImageOrderAsc(post)
                .stream()
                .map(CommunityImageEntity::getImageUrl)
                .toList();

        return CommunityResponse.from(post, imageUrls);
    }
}
