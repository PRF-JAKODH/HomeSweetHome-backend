package com.homesweet.homesweetback.domain.community.service;

import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.repository.UserRepository;
import com.homesweet.homesweetback.domain.community.dto.CommunityPostRequest;
import com.homesweet.homesweetback.domain.community.dto.CommunityPostResponse;
import com.homesweet.homesweetback.domain.community.entity.CommunityPostEntity;
import com.homesweet.homesweetback.domain.community.repository.CommunityImageRepository;
import com.homesweet.homesweetback.domain.community.repository.CommunityPostRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;
import org.springframework.web.multipart.MultipartFile;

import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommunityPostServiceTest {

    @Mock
    private CommunityPostRepository postRepository;

    @Mock
    private CommunityImageRepository imageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CommunityImageUploader imageUploader;

    @InjectMocks
    private CommunityPostService communityPostService;


    // ArgumentCaptor를 사용할수도 있다

    @DisplayName("게시물 생성 테스트")
    @Test
    void postPost() {
        // given
        Long userId = 1L;
        CommunityPostRequest request = new CommunityPostRequest("Test Title", "Test Content", "Test category");

        User fakeUser = User.builder().id(userId).name("fakeUser").build();

        CommunityPostEntity savedPost = CommunityPostEntity.builder()
                .postId(1L)
                .author(fakeUser)
                .title(request.title())
                .content(request.content())
                .category(request.category())
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(fakeUser));
        when(postRepository.save(any(CommunityPostEntity.class))).thenReturn(savedPost);

        // when
        CommunityPostResponse response = communityPostService.createPost(Collections.emptyList(), request, userId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.title()).isEqualTo("Test Title");
        assertThat(response.content()).isEqualTo("Test Content");
        assertThat(response.category()).isEqualTo("Test category");

        verify(userRepository).findById(userId);
        verify(postRepository).save(any(CommunityPostEntity.class));
    }

    @DisplayName("게시물 조회 테스트")
    @Test
    void getPost() {
        // given
        Long postId = 1L;
        User fakeUser = User.builder().id(1L).name("fakeUser").build();
        CommunityPostEntity fakePost = CommunityPostEntity.builder()
                .postId(postId)
                .author(fakeUser)
                .title("Test Title")
                .content("Test Content")
                .category("Test Category")
                .viewCount(0)
                .likeCount(0)
                .commentCount(0)
                .isModified(false)
                .createdAt(java.time.LocalDateTime.now())
                .modifiedAt(null)
                .build();

        when(postRepository.findByPostIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(fakePost));
        when(imageRepository.findByPostOrderByImageOrderAsc(any(CommunityPostEntity.class))).thenReturn(Collections.emptyList());

        // when
        CommunityPostResponse response = communityPostService.getPost(postId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.postId()).isEqualTo(postId);
        assertThat(response.title()).isEqualTo("Test Title");
        assertThat(response.content()).isEqualTo("Test Content");
        assertThat(response.category()).isEqualTo("Test Category");

        verify(imageRepository).findByPostOrderByImageOrderAsc(any(CommunityPostEntity.class));
    }

    @DisplayName("게시물 수정 테스트")
    @Test
    void updatePost() {
        // given
        Long postId = 2L;
        Long userId = 2L;
        User fakeUser = User.builder().id(userId).name("User").build();
        CommunityPostEntity originalPost = CommunityPostEntity.builder()
                .postId(postId)
                .author(fakeUser)
                .title("원본 제목")
                .content("원본 내용")
                .category("원본 카테고리")
                .viewCount(0)
                .likeCount(0)
                .commentCount(0)
                .isModified(false)
                .createdAt(java.time.LocalDateTime.now())
                .modifiedAt(null)
                .build();

        CommunityPostRequest updateRequest = new CommunityPostRequest("수정된 제목", "수정된 내용", "수정된 카테고리");

        when(postRepository.findByPostIdAndIsDeletedFalse(postId)).thenReturn(Optional.of(originalPost));
        when(imageRepository.findByPostOrderByImageOrderAsc(any(CommunityPostEntity.class))).thenReturn(Collections.emptyList());

        // when
        CommunityPostResponse response = communityPostService.updatePost(postId, updateRequest, userId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.title()).isEqualTo("수정된 제목");
        assertThat(response.content()).isEqualTo("수정된 내용");
        assertThat(response.category()).isEqualTo("수정된 카테고리");
        assertThat(response.isModified()).isTrue();

        verify(postRepository).findByPostIdAndIsDeletedFalse(postId);
    }

    @DisplayName("게시물 삭제 테스트")
    @Test
    void deletePost() {
        // given
        Long postId = 2L;
        Long userId = 2L;
        User fakeUser = User.builder().id(userId).name("User").build();
        CommunityPostEntity originalPost = CommunityPostEntity.builder()
                .postId(postId)
                .author(fakeUser)
                .title("원본 제목")
                .content("원본 내용")
                .category("원본 카테고리")
                .viewCount(0)
                .likeCount(0)
                .commentCount(0)
                .isModified(false)
                .createdAt(java.time.LocalDateTime.now())
                .modifiedAt(null)
                .build();

        when(postRepository.findByPostIdAndIsDeletedFalse(postId)).thenReturn(Optional.of(originalPost));

        // when
        communityPostService.deletePost(postId, userId);      //  response 반환값없음

        // then
        verify(postRepository).findByPostIdAndIsDeletedFalse(postId);
        assertThat(originalPost.getIsDeleted()).isTrue();
    }
}