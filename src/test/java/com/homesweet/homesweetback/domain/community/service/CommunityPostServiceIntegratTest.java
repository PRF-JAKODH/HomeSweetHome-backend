package com.homesweet.homesweetback.domain.community.service;

import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.common.s3.impl.S3ImageUploader;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.repository.UserRepository;
import com.homesweet.homesweetback.domain.community.dto.CommunityPostRequest;
import com.homesweet.homesweetback.domain.community.dto.CommunityPostResponse;
import com.homesweet.homesweetback.domain.community.dto.exception.CommunityException;
import com.homesweet.homesweetback.domain.community.entity.CommunityPostEntity;
import com.homesweet.homesweetback.domain.community.repository.CommunityImageRepository;
import com.homesweet.homesweetback.domain.community.repository.CommunityPostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import com.homesweet.homesweetback.domain.auth.entity.OAuth2Provider;
import com.homesweet.homesweetback.domain.auth.entity.UserRole;
import com.homesweet.homesweetback.domain.community.entity.CommunityImageEntity;
import com.homesweet.homesweetback.domain.notification.service.NotificationSendService;
import io.awspring.cloud.s3.S3Template;

/**
 * - 실제 DB(H2)를 사용하여 전체 플로우 검증
 * - 트랜잭션 롤백으로 테스트 격리 보장
 * - S3Template, S3ImageUploader는 다른 서비스(ChatRoomServiceImpl 등)에서 직접 의존하므로 MockitoBean 필요
 * - CommunityImageUploader를 Mock하여 S3 업로드를 시뮬레이션
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CommunityPostServiceIntegratTest {

    @Autowired
    private CommunityPostService communityPostService;

    @Autowired
    private CommunityPostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CommunityImageRepository imageRepository;

    @MockitoBean
    private S3Template s3Template;

    @MockitoBean
    private S3ImageUploader s3ImageUploader;

    @MockitoBean
    private CommunityImageUploader communityImageUploader;

    @MockitoBean
    private NotificationSendService notificationSendService;

    private User testUser;
    private User anotherUser;

    @BeforeEach
    void setUp() {
        // 테스트용 사용자 생성
        testUser = User.builder()
                .email("test@example.com")
                .name("테스트유저")
                .profileImageUrl("http://example.com/profile.jpg")
                .provider(OAuth2Provider.GOOGLE)
                .role(UserRole.USER)
                .build();
        testUser = userRepository.save(testUser);

        anotherUser = User.builder()
                .email("another@example.com")
                .name("다른유저")
                .profileImageUrl("http://example.com/profile2.jpg")
                .provider(OAuth2Provider.GOOGLE)
                .role(UserRole.USER)
                .build();
        anotherUser = userRepository.save(anotherUser);

        // Mock CommunityImageUploader - S3 업로드를 시뮬레이션
        when(communityImageUploader.uploadCommunityImages(anyList()))
                .thenAnswer(invocation -> {
                    List<MultipartFile> files = invocation.getArgument(0);
                    return files.stream()
                            .map(file -> "https://test-bucket.s3.amazonaws.com/community/" + file.getOriginalFilename())
                            .collect(Collectors.toList());
                });
    }

    @Test
    @DisplayName("게시글 작성 성공")
    void createPost_Success() {
        // given
        CommunityPostRequest request = new CommunityPostRequest(
                "테스트 제목",
                "테스트 내용입니다.",
                "자유게시판"
        );

        // when
        CommunityPostResponse response = communityPostService.createPost(null, request, testUser.getId());

        // then
        assertThat(response).isNotNull();
        assertThat(response.title()).isEqualTo("테스트 제목");
        assertThat(response.content()).isEqualTo("테스트 내용입니다.");
        assertThat(response.category()).isEqualTo("자유게시판");
        assertThat(response.authorName()).isEqualTo("테스트유저");
        assertThat(response.viewCount()).isZero();
        assertThat(response.likeCount()).isZero();
        assertThat(response.commentCount()).isZero();
    }

    @Test
    @DisplayName("게시글 작성 실패 - 존재하지 않는 사용자")
    void createPost_Fail_UserNotFound() {
        // given
        CommunityPostRequest request = new CommunityPostRequest(
                "테스트 제목",
                "테스트 내용입니다.",
                "자유게시판"
        );
        Long invalidUserId = 99999L;

        // when & then
        assertThatThrownBy(() -> communityPostService.createPost(null, request, invalidUserId))
                .isInstanceOf(CommunityException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("게시글 단건 조회 성공")
    void getPost_Success() {
        // given
        CommunityPostEntity post = createTestPost("조회 테스트", "내용", testUser);

        // when
        CommunityPostResponse response = communityPostService.getPost(post.getPostId());

        // then
        assertThat(response).isNotNull();
        assertThat(response.postId()).isEqualTo(post.getPostId());
        assertThat(response.title()).isEqualTo("조회 테스트");
        assertThat(response.content()).isEqualTo("내용");
    }

    @Test
    @DisplayName("게시글 조회 실패 - 존재하지 않는 게시글")
    void getPost_Fail_NotFound() {
        // given
        Long invalidPostId = 99999L;

        // when & then
        assertThatThrownBy(() -> communityPostService.getPost(invalidPostId))
                .isInstanceOf(CommunityException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COMMUNITY_POST_NOT_FOUND);
    }

    @Test
    @DisplayName("게시글 조회 실패 - 삭제된 게시글")
    void getPost_Fail_DeletedPost() {
        // given
        CommunityPostEntity post = createTestPost("삭제될 게시글", "내용", testUser);
        post.deletePost();
        postRepository.save(post);

        // when & then
        assertThatThrownBy(() -> communityPostService.getPost(post.getPostId()))
                .isInstanceOf(CommunityException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COMMUNITY_POST_NOT_FOUND);
    }

    @Test
    @DisplayName("게시글 수정 성공")
    void updatePost_Success() {
        // given
        CommunityPostEntity post = createTestPost("원본 제목", "원본 내용", testUser);
        CommunityPostRequest updateRequest = new CommunityPostRequest(
                "수정된 제목",
                "수정된 내용",
                "공지사항"
        );

        // when
        CommunityPostResponse response = communityPostService.updatePost(
                post.getPostId(),
                updateRequest,
                testUser.getId()
        );

        // then
        assertThat(response.title()).isEqualTo("수정된 제목");
        assertThat(response.content()).isEqualTo("수정된 내용");
        assertThat(response.category()).isEqualTo("공지사항");
    }

    @Test
    @DisplayName("게시글 수정 실패 - 작성자가 아닌 사용자")
    void updatePost_Fail_NotAuthor() {
        // given
        CommunityPostEntity post = createTestPost("원본 제목", "원본 내용", testUser);
        CommunityPostRequest updateRequest = new CommunityPostRequest(
                "수정된 제목",
                "수정된 내용",
                "공지사항"
        );

        // when & then
        assertThatThrownBy(() -> communityPostService.updatePost(
                post.getPostId(),
                updateRequest,
                anotherUser.getId()
        ))
                .isInstanceOf(CommunityException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COMMUNITY_POST_FORBIDDEN);
    }

    @Test
    @DisplayName("게시글 수정 실패 - 삭제된 게시글")
    void updatePost_Fail_DeletedPost() {
        // given
        CommunityPostEntity post = createTestPost("원본 제목", "원본 내용", testUser);
        post.deletePost();
        postRepository.save(post);

        CommunityPostRequest updateRequest = new CommunityPostRequest(
                "수정된 제목",
                "수정된 내용",
                "공지사항"
        );

        // when & then
        assertThatThrownBy(() -> communityPostService.updatePost(
                post.getPostId(),
                updateRequest,
                testUser.getId()
        ))
                .isInstanceOf(CommunityException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COMMUNITY_POST_NOT_FOUND);
    }

    @Test
    @DisplayName("게시글 삭제 성공")
    void deletePost_Success() {
        // given
        CommunityPostEntity post = createTestPost("삭제할 게시글", "내용", testUser);
        Long postId = post.getPostId();

        // when
        communityPostService.deletePost(postId, testUser.getId());

        // then
        CommunityPostEntity deletedPost = postRepository.findById(postId).orElseThrow();
        assertThat(deletedPost.getIsDeleted()).isTrue();
    }

    @Test
    @DisplayName("게시글 삭제 실패 - 작성자가 아닌 사용자")
    void deletePost_Fail_NotAuthor() {
        // given
        CommunityPostEntity post = createTestPost("삭제할 게시글", "내용", testUser);

        // when & then
        assertThatThrownBy(() -> communityPostService.deletePost(
                post.getPostId(),
                anotherUser.getId()
        ))
                .isInstanceOf(CommunityException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COMMUNITY_POST_FORBIDDEN);
    }

    @Test
    @DisplayName("게시글 목록 조회 성공 - 페이지네이션")
    void getPosts_Success_WithPagination() {
        // given
        for (int i = 1; i <= 15; i++) {
            createTestPost("게시글 " + i, "내용 " + i, testUser);
        }

        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        // when
        Page<CommunityPostResponse> result = communityPostService.getPosts(pageable);

        // then
        assertThat(result.getContent()).hasSize(10);
        assertThat(result.getTotalElements()).isEqualTo(15);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.getNumber()).isZero(); // 첫 페이지
    }

    @Test
    @DisplayName("게시글 목록 조회 - 삭제된 게시글 제외")
    void getPosts_ExcludeDeletedPosts() {
        // given
        createTestPost("일반 게시글 1", "내용 1", testUser);
        CommunityPostEntity deletedPost = createTestPost("삭제될 게시글", "내용", testUser);
        deletedPost.deletePost();
        postRepository.save(deletedPost);
        createTestPost("일반 게시글 2", "내용 2", testUser);

        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        // when
        Page<CommunityPostResponse> result = communityPostService.getPosts(pageable);

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
                .extracting(CommunityPostResponse::title)
                .containsExactlyInAnyOrder("일반 게시글 1", "일반 게시글 2");
    }

    @Test
    @DisplayName("게시글 목록 조회 - 빈 결과")
    void getPosts_EmptyResult() {
        // given
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        // when
        Page<CommunityPostResponse> result = communityPostService.getPosts(pageable);

        // then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    // ==================== 이미지 업로드 테스트 ====================

    @Test
    @DisplayName("게시글 작성 성공 - 단일 이미지 업로드")
    void createPost_WithSingleImage() {
        // given
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "test-image.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );
        CommunityPostRequest request = new CommunityPostRequest(
                "이미지 포함 게시글",
                "이미지가 포함된 내용입니다.",
                "자유게시판"
        );

        // when
        CommunityPostResponse response = communityPostService.createPost(
                Arrays.asList(image),
                request,
                testUser.getId()
        );

        // then
        assertThat(response.imagesUrl()).isNotNull();
        assertThat(response.imagesUrl()).hasSize(1);
        assertThat(response.imagesUrl().get(0)).contains("test-bucket.s3.amazonaws.com");

        // DB에 이미지 정보가 저장되었는지 확인
        List<CommunityImageEntity> savedImages = imageRepository.findByPostOrderByImageOrderAsc(
                postRepository.findById(response.postId()).orElseThrow()
        );
        assertThat(savedImages).hasSize(1);
        assertThat(savedImages.get(0).getImageUrl()).contains("community");
        assertThat(savedImages.get(0).getImageOrder()).isEqualTo(1);
    }

    @Test
    @DisplayName("게시글 작성 성공 - 다중 이미지 업로드")
    void createPost_WithMultipleImages() {
        // given
        MockMultipartFile image1 = new MockMultipartFile(
                "image1", "test1.jpg", "image/jpeg", "image1".getBytes()
        );
        MockMultipartFile image2 = new MockMultipartFile(
                "image2", "test2.jpg", "image/jpeg", "image2".getBytes()
        );
        MockMultipartFile image3 = new MockMultipartFile(
                "image3", "test3.jpg", "image/jpeg", "image3".getBytes()
        );
        CommunityPostRequest request = new CommunityPostRequest(
                "다중 이미지 게시글",
                "여러 이미지가 포함된 게시글입니다.",
                "자유게시판"
        );

        // when
        CommunityPostResponse response = communityPostService.createPost(
                Arrays.asList(image1, image2, image3),
                request,
                testUser.getId()
        );

        // then
        assertThat(response.imagesUrl()).hasSize(3);

        // 이미지 순서 확인
        List<CommunityImageEntity> savedImages = imageRepository.findByPostOrderByImageOrderAsc(
                postRepository.findById(response.postId()).orElseThrow()
        );
        assertThat(savedImages).hasSize(3);
        assertThat(savedImages.get(0).getImageOrder()).isEqualTo(1);
        assertThat(savedImages.get(1).getImageOrder()).isEqualTo(2);
        assertThat(savedImages.get(2).getImageOrder()).isEqualTo(3);
    }

    @Test
    @DisplayName("게시글 작성 성공 - 이미지 없이")
    void createPost_WithoutImage() {
        // given
        CommunityPostRequest request = new CommunityPostRequest(
                "이미지 없는 게시글",
                "이미지가 없는 내용입니다.",
                "자유게시판"
        );

        // when
        CommunityPostResponse response = communityPostService.createPost(
                null,
                request,
                testUser.getId()
        );

        // then
        assertThat(response.imagesUrl()).isEmpty();

        // DB에 이미지가 저장되지 않았는지 확인
        List<CommunityImageEntity> savedImages = imageRepository.findByPostOrderByImageOrderAsc(
                postRepository.findById(response.postId()).orElseThrow()
        );
        assertThat(savedImages).isEmpty();
    }

    @Test
    @DisplayName("게시글 조회 성공 - 이미지 포함")
    void getPost_WithImages() {
        // given
        MockMultipartFile image1 = new MockMultipartFile(
                "image1", "test1.jpg", "image/jpeg", "image1".getBytes()
        );
        MockMultipartFile image2 = new MockMultipartFile(
                "image2", "test2.jpg", "image/jpeg", "image2".getBytes()
        );
        CommunityPostRequest createRequest = new CommunityPostRequest(
                "이미지 조회 테스트",
                "내용",
                "자유게시판"
        );
        CommunityPostResponse createdPost = communityPostService.createPost(
                Arrays.asList(image1, image2),
                createRequest,
                testUser.getId()
        );

        // when
        CommunityPostResponse response = communityPostService.getPost(createdPost.postId());

        // then
        assertThat(response.imagesUrl()).hasSize(2);
        assertThat(response.imagesUrl()).containsExactlyElementsOf(createdPost.imagesUrl());
    }

    @Test
    @DisplayName("게시글 목록 조회 - 이미지 포함")
    void getPosts_WithImages() {
        // given
        MockMultipartFile image = new MockMultipartFile(
                "image", "test.jpg", "image/jpeg", "image".getBytes()
        );
        CommunityPostRequest request1 = new CommunityPostRequest(
                "이미지 있는 게시글", "내용1", "자유게시판"
        );
        CommunityPostRequest request2 = new CommunityPostRequest(
                "이미지 없는 게시글", "내용2", "자유게시판"
        );

        communityPostService.createPost(Arrays.asList(image), request1, testUser.getId());
        communityPostService.createPost(null, request2, testUser.getId());

        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        // when
        Page<CommunityPostResponse> result = communityPostService.getPosts(pageable);

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).imagesUrl()).isEmpty(); // 최신순이므로 이미지 없는 게시글
        assertThat(result.getContent().get(1).imagesUrl()).hasSize(1); // 이미지 있는 게시글
    }

    @Test
    @DisplayName("게시글 삭제 - 이미지가 포함된 게시글")
    void deletePost_WithImages() {
        // given
        MockMultipartFile image = new MockMultipartFile(
                "image", "test.jpg", "image/jpeg", "image".getBytes()
        );
        CommunityPostRequest request = new CommunityPostRequest(
                "삭제할 게시글", "내용", "자유게시판"
        );
        CommunityPostResponse createdPost = communityPostService.createPost(
                Arrays.asList(image),
                request,
                testUser.getId()
        );

        // when
        communityPostService.deletePost(createdPost.postId(), testUser.getId());

        // then
        CommunityPostEntity deletedPost = postRepository.findById(createdPost.postId()).orElseThrow();
        assertThat(deletedPost.getIsDeleted()).isTrue();

        // 이미지는 여전히 DB에 존재 (소프트 삭제)
        List<CommunityImageEntity> images = imageRepository.findByPostOrderByImageOrderAsc(deletedPost);
        assertThat(images).hasSize(1);
    }

    /**
     * 테스트용 게시글 생성 헬퍼 메소드
     */
    private CommunityPostEntity createTestPost(String title, String content, User author) {
        CommunityPostEntity post = CommunityPostEntity.builder()
                .author(author)
                .title(title)
                .content(content)
                .category("자유게시판")
                .build();
        return postRepository.save(post);
    }

    /**
     * 테스트용 MockMultipartFile 생성 헬퍼 메소드
     */
    private MockMultipartFile createMockImage(String fileName) {
        return new MockMultipartFile(
                "image",
                fileName,
                "image/jpeg",
                "test image content".getBytes()
        );
    }
}
