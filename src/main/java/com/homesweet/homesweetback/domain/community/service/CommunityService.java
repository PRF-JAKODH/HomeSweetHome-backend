package com.homesweet.homesweetback.domain.community.service;

import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.repository.UserRepository;
import com.homesweet.homesweetback.domain.community.dto.CommunityCreateRequest;
import com.homesweet.homesweetback.domain.community.dto.CommunityResponse;
import com.homesweet.homesweetback.domain.community.entity.CommunityPostEntity;
import com.homesweet.homesweetback.domain.community.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final CommunityCommentRepository commentRepository;
    private final CommunityImageRepository imageRepository;
    private final CommunityPostLikeRepository likeRepository;
    private final CommunityCommentLikeRepository commentLikeRepository;
    private final UserRepository userRepository;

    /**
     * 게시글 작성
     */
    @Transactional
    public CommunityResponse createPost(CommunityCreateRequest request, Long userId) {
        // TODO: Grade 엔티티 이슈 해결 후 User 연관관계로 변경 필요
        // User author = userRepository.findById(userId)
        //         .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        // userId 존재 여부 확인
        if (!userRepository.existsById(userId)) {
            throw new EntityNotFoundException("사용자를 찾을 수 없습니다.");
        }

        CommunityPostEntity post = CommunityPostEntity.builder()
                // .author(author)  // TODO: Grade 이슈 해결 후 복구
                .userId(userId)
                .title(request.title())
                .content(request.content())
                .build();

        CommunityPostEntity savedPost = postRepository.save(post);

        return CommunityResponse.from(savedPost);
    }

    /**
     * 게시글 단건 조회
     */
    public CommunityResponse getPost(Long postId) {
        CommunityPostEntity post = postRepository.findByPostIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new EntityNotFoundException("게시글을 찾을 수 없습니다."));

        return CommunityResponse.from(post);
    }
}
