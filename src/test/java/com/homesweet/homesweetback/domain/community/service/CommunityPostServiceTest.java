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


    /**
     * any()로 짬 다음부턴 @argumentCaptor로
     */
    @DisplayName("게시물 생성 테스트")
    @Test
    void postPost(){
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
    void getPost(){

    }

    @DisplayName("게시물 수정 테스트")
    @Test
    void updatePost(){
        // ArgumentCaptor 생성
        ArgumentCaptor<CommunityPostEntity> getCaptor = ArgumentCaptor.forClass((CommunityPostEntity.class));

        // given
        Long userId = 1L;
        CommunityPostRequest request = new CommunityPostRequest("Test Title", "Test Content", "Test category");

        User fakeUser = User.builder().id(userId).name("fakeUser").build();

//        CommunityPostEntity savedPost = CommunityPostEntity.builder()
//                .postId(1L)
//                .author(fakeUser)
//                .title(request.title())
//                .content(request.content())
//                .category(request.category())
//                .build();

        communityPostService.createPost(Collections.emptyList(), request, userId);
        
        // when
        when(userRepository.findById(userId)).thenReturn(Optional.of(fakeUser));
        when(postRepository.save(any(CommunityPostEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // then
        verify(postRepository).save((getCaptor.capture()));

        CommunityPostEntity getPost = getCaptor.getValue();

        assertThat(getPost.getTitle()).isEqualTo("Test Title");
        assertThat(getPost.getContent()).isEqualTo("Test Content");
        assertThat(getPost.getCategory()).isEqualTo("Test category");
        assertThat(getPost.getAuthor()).isEqualTo(fakeUser);
        assertThat(getPost.getIsDeleted()).isFalse();
    }
}