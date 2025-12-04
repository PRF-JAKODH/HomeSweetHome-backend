package com.homesweet.homesweetback.domain.community.repository;

import com.homesweet.homesweetback.domain.auth.entity.OAuth2Provider;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.entity.UserRole;
import com.homesweet.homesweetback.domain.auth.repository.UserRepository;
import com.homesweet.homesweetback.domain.community.entity.*;
import com.homesweet.homesweetback.domain.community.repository.*;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CommunityRepositoryTest {

    @Autowired
    private CommunityPostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CommunityCommentRepository commentRepository;

    @Autowired
    private CommunityPostLikeRepository postLikeRepository;

    @Autowired
    private CommunityImageRepository imageRepository;

    @Test
    @DisplayName("게시글 저장이 잘되는지 확인")
    void savePost() {
        // given: 테스트 데이터

        // user 먼저 생성
        User testUser = User.builder()
                .email("test@test.com")
                .name("tester")
                .provider(OAuth2Provider.GOOGLE)
                .role(UserRole.USER)
                .build();
        User SavedUser = userRepository.save(testUser);

        // post 생성
        String title = "테스트 제목";
        String content = "테스트 내용";
        String category = "게시글 유형";

        CommunityPostEntity post = CommunityPostEntity.builder()
                .title(title)
                .content(content)
                .category(category)
                .author(SavedUser)
                .build();

        // when
        CommunityPostEntity savedPost = postRepository.save(post);

        // then
        assertThat(savedPost.getPostId()).isNotNull();
        assertThat(savedPost.getTitle()).isEqualTo(title);
        assertThat(savedPost.getContent()).isEqualTo(content);
        assertThat(savedPost.getCategory()).isEqualTo(category);
        assertThat(savedPost.getIsDeleted()).isFalse();
    }

    @Test
    @DisplayName("게시글 조회가 잘되는지 확인")
    void getPost() {
        // given: 유저와 게시물 생성
        User testUser = User.builder()
                .email("test@test.com")
                .name("tester")
                .provider(OAuth2Provider.GOOGLE)
                .role(UserRole.USER)
                .build();
        User SavedUser = userRepository.save(testUser);

        String title = "테스트 제목";
        String content = "테스트 내용";
        String category = "게시글 유형";

        CommunityPostEntity post = CommunityPostEntity.builder()
                .title(title)
                .content(content)
                .category(category)
                .author(SavedUser)
                .build();

        CommunityPostEntity savedPost = postRepository.save(post);

        Long postId = savedPost.getPostId();

        // when: 게시물 조회
        Optional<CommunityPostEntity> result = postRepository.findById(postId);

        // then: 조회 검증
        assertThat(result).isPresent();
        assertThat(result.get().getPostId()).isEqualTo(postId);
        assertThat(result.get().getTitle()).isEqualTo(title);
        assertThat(result.get().getContent()).isEqualTo(content);
        assertThat(result.get().getCategory()).isEqualTo(category);
        assertThat(result.get().getIsDeleted()).isFalse();
        assertThat(result.get().getAuthor()).isEqualTo(SavedUser);
        assertThat(result.get().getAuthor().getEmail()).isEqualTo(testUser.getEmail());
        assertThat(result.get().getAuthor().getName()).isEqualTo(testUser.getName());
        assertThat(result.get().getAuthor().getProvider()).isEqualTo(testUser.getProvider());
        assertThat(result.get().getAuthor().getRole()).isEqualTo(testUser.getRole());
    }

    @Test
    @DisplayName("게시글 수정이 잘되는지 확인")
    void updatePost() {
        // given: 유저와 게시물 생성
        User testUser = User.builder()
                .email("test@test.com")
                .name("tester")
                .provider(OAuth2Provider.GOOGLE)
                .role(UserRole.USER)
                .build();
        User SavedUser = userRepository.save(testUser);

        String title = "테스트 제목";
        String content = "테스트 내용";
        String category = "게시글 유형";

        CommunityPostEntity post = CommunityPostEntity.builder()
                .title(title)
                .content(content)
                .category(category)
                .author(SavedUser)
                .build();

        CommunityPostEntity savedPost = postRepository.save(post);


        // 게시글 수정로직
        Long postId = savedPost.getPostId();

        String updatedTitle = "수정된 테스트 제목";
        String updatedContent = "수정된 테스트 내용";
        String updatedCategory = "공지";

        savedPost.updatePost(updatedTitle, updatedContent, updatedCategory);

        postRepository.save(savedPost);

        // when:
        Optional<CommunityPostEntity> result = postRepository.findById(postId);

        // then:
        assertThat(result).isPresent();
        assertThat(result.get().getPostId()).isEqualTo(postId);
        assertThat(result.get().getTitle()).isEqualTo(updatedTitle);
        assertThat(result.get().getContent()).isEqualTo(updatedContent);
        assertThat(result.get().getCategory()).isEqualTo(updatedCategory);
    }

    @Test
    @DisplayName("게시글 삭제가 잘되는지 확인")
    void deletePost() {
        // given: 테스트 데이터

        // user 먼저 생성
        User testUser = User.builder()
                .email("test@test.com")
                .name("tester")
                .provider(OAuth2Provider.GOOGLE)
                .role(UserRole.USER)
                .build();
        User SavedUser = userRepository.save(testUser);

        // post 생성
        String title = "테스트 제목";
        String content = "테스트 내용";
        String category = "게시글 유형";

        CommunityPostEntity post = CommunityPostEntity.builder()
                .title(title)
                .content(content)
                .category(category)
                .author(SavedUser)
                .build();

        CommunityPostEntity savedPost = postRepository.save(post);

        // when:
        postRepository.delete(savedPost);

        // then:
        assertThat(postRepository.findById(post.getPostId())).isEmpty();
    }

    @Test
    @DisplayName("댓글 저장이 잘되는지 확인")
    void saveComment() {
        // given: 유저와 게시글 생성
        User testUser = User.builder()
                .email("test@test.com")
                .name("tester")
                .provider(OAuth2Provider.GOOGLE)
                .role(UserRole.USER)
                .build();
        User savedUser = userRepository.save(testUser);

        CommunityPostEntity post = CommunityPostEntity.builder()
                .title("테스트 제목")
                .content("테스트 내용")
                .category("게시글 유형")
                .author(savedUser)
                .build();
        CommunityPostEntity savedPost = postRepository.save(post);

        // 댓글 생성
        String commentContent = "테스트 댓글입니다.";
        CommunityCommentEntity comment = CommunityCommentEntity.builder()
                .post(savedPost)
                .author(savedUser)
                .content(commentContent)
                .build();

        // when: 댓글 저장
        CommunityCommentEntity savedComment = commentRepository.save(comment);

        // then: 댓글 검증
        assertThat(savedComment.getCommentId()).isNotNull();
        assertThat(savedComment.getContent()).isEqualTo(commentContent);
        assertThat(savedComment.getPost().getPostId()).isEqualTo(savedPost.getPostId());
        assertThat(savedComment.getAuthor().getId()).isEqualTo(savedUser.getId());
        assertThat(savedComment.getIsDeleted()).isFalse();
        assertThat(savedComment.getLikeCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("특정 게시글의 댓글 조회가 잘되는지 확인")
    void findCommentsByPostId() {
        // given: 유저와 게시글 생성
        User testUser = User.builder()
                .email("test@test.com")
                .name("tester")
                .provider(OAuth2Provider.GOOGLE)
                .role(UserRole.USER)
                .build();
        User savedUser = userRepository.save(testUser);

        CommunityPostEntity post = CommunityPostEntity.builder()
                .title("테스트 제목")
                .content("테스트 내용")
                .category("게시글 유형")
                .author(savedUser)
                .build();
        CommunityPostEntity savedPost = postRepository.save(post);

        // 여러 댓글 생성
        CommunityCommentEntity comment1 = CommunityCommentEntity.builder()
                .post(savedPost)
                .author(savedUser)
                .content("첫 번째 댓글")
                .build();
        CommunityCommentEntity comment2 = CommunityCommentEntity.builder()
                .post(savedPost)
                .author(savedUser)
                .content("두 번째 댓글")
                .build();

        commentRepository.save(comment1);
        commentRepository.save(comment2);

        // when: 게시글의 댓글 조회
        List<CommunityCommentEntity> comments = commentRepository.findByPost_PostIdAndIsDeletedFalse(savedPost.getPostId());

        // then: 댓글 목록 검증
        assertThat(comments).hasSize(2);
        assertThat(comments).extracting(CommunityCommentEntity::getContent)
                .containsExactlyInAnyOrder("첫 번째 댓글", "두 번째 댓글");
    }

    @Test
    @DisplayName("댓글 수정이 잘되는지 확인")
    void updateComment() {
        // given: 유저, 게시글, 댓글 생성
        User testUser = User.builder()
                .email("test@test.com")
                .name("tester")
                .provider(OAuth2Provider.GOOGLE)
                .role(UserRole.USER)
                .build();
        User savedUser = userRepository.save(testUser);

        CommunityPostEntity post = CommunityPostEntity.builder()
                .title("테스트 제목")
                .content("테스트 내용")
                .category("게시글 유형")
                .author(savedUser)
                .build();
        CommunityPostEntity savedPost = postRepository.save(post);

        CommunityCommentEntity comment = CommunityCommentEntity.builder()
                .post(savedPost)
                .author(savedUser)
                .content("원래 댓글")
                .build();
        CommunityCommentEntity savedComment = commentRepository.save(comment);

        // when: 댓글 수정
        String updatedContent = "수정된 댓글";
        savedComment.updateComment(updatedContent);
        commentRepository.save(savedComment);

        // then: 수정 검증
        Optional<CommunityCommentEntity> result = commentRepository.findById(savedComment.getCommentId());
        assertThat(result).isPresent();
        assertThat(result.get().getContent()).isEqualTo(updatedContent);
        assertThat(result.get().getIsModified()).isTrue();
        assertThat(result.get().getModifiedAt()).isNotNull();
    }

    @Test
    @DisplayName("댓글 삭제가 잘되는지 확인")
    void deleteComment() {
        // given: 유저, 게시글, 댓글 생성
        User testUser = User.builder()
                .email("test@test.com")
                .name("tester")
                .provider(OAuth2Provider.GOOGLE)
                .role(UserRole.USER)
                .build();
        User savedUser = userRepository.save(testUser);

        CommunityPostEntity post = CommunityPostEntity.builder()
                .title("테스트 제목")
                .content("테스트 내용")
                .category("게시글 유형")
                .author(savedUser)
                .build();
        CommunityPostEntity savedPost = postRepository.save(post);

        CommunityCommentEntity comment = CommunityCommentEntity.builder()
                .post(savedPost)
                .author(savedUser)
                .content("삭제할 댓글")
                .build();
        CommunityCommentEntity savedComment = commentRepository.save(comment);

        // when: 댓글 삭제
        commentRepository.delete(savedComment);

        // then: 삭제 검증
        assertThat(commentRepository.findById(savedComment.getCommentId())).isEmpty();
    }

    @Test
    @DisplayName("게시글 좋아요 저장이 잘되는지 확인")
    void savePostLike() {
        // given: 유저와 게시글 생성
        User testUser = User.builder()
                .email("test@test.com")
                .name("tester")
                .provider(OAuth2Provider.GOOGLE)
                .role(UserRole.USER)
                .build();
        User savedUser = userRepository.save(testUser);

        CommunityPostEntity post = CommunityPostEntity.builder()
                .title("테스트 제목")
                .content("테스트 내용")
                .category("게시글 유형")
                .author(savedUser)
                .build();
        CommunityPostEntity savedPost = postRepository.save(post);

        // 좋아요 생성
        CommunityPostLikeEntity postLike = CommunityPostLikeEntity.builder()
                .post(savedPost)
                .user(savedUser)
                .build();

        // when: 좋아요 저장
        CommunityPostLikeEntity savedPostLike = postLikeRepository.save(postLike);

        // then: 좋아요 검증
        assertThat(savedPostLike.getPost().getPostId()).isEqualTo(savedPost.getPostId());
        assertThat(savedPostLike.getUser().getId()).isEqualTo(savedUser.getId());
        assertThat(savedPostLike.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("게시글 좋아요 존재 여부 확인")
    void existsPostLike() {
        // given: 유저와 게시글 생성
        User testUser = User.builder()
                .email("test@test.com")
                .name("tester")
                .provider(OAuth2Provider.GOOGLE)
                .role(UserRole.USER)
                .build();
        User savedUser = userRepository.save(testUser);

        CommunityPostEntity post = CommunityPostEntity.builder()
                .title("테스트 제목")
                .content("테스트 내용")
                .category("게시글 유형")
                .author(savedUser)
                .build();
        CommunityPostEntity savedPost = postRepository.save(post);

        // 좋아요 생성
        CommunityPostLikeEntity postLike = CommunityPostLikeEntity.builder()
                .post(savedPost)
                .user(savedUser)
                .build();
        postLikeRepository.save(postLike);

        // when: 좋아요 존재 확인
        boolean exists = postLikeRepository.existsByPost_PostIdAndUser_Id(savedPost.getPostId(), savedUser.getId());

        // then: 존재 검증
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("게시글 좋아요 삭제가 잘되는지 확인")
    void deletePostLike() {
        // given: 유저, 게시글, 좋아요 생성
        User testUser = User.builder()
                .email("test@test.com")
                .name("tester")
                .provider(OAuth2Provider.GOOGLE)
                .role(UserRole.USER)
                .build();
        User savedUser = userRepository.save(testUser);

        CommunityPostEntity post = CommunityPostEntity.builder()
                .title("테스트 제목")
                .content("테스트 내용")
                .category("게시글 유형")
                .author(savedUser)
                .build();
        CommunityPostEntity savedPost = postRepository.save(post);

        CommunityPostLikeEntity postLike = CommunityPostLikeEntity.builder()
                .post(savedPost)
                .user(savedUser)
                .build();
        postLikeRepository.save(postLike);

        // when: 좋아요 삭제
        int deleted = postLikeRepository.deleteByPostIdAndUserId(savedPost.getPostId(), savedUser.getId());

        // then: 삭제 검증
        assertThat(deleted).isEqualTo(1);
        boolean exists = postLikeRepository.existsByPost_PostIdAndUser_Id(savedPost.getPostId(), savedUser.getId());
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("게시글 조회수 증가가 잘되는지 확인")
    void increaseViewCount() {
        // given: 유저와 게시글 생성
        User testUser = User.builder()
                .email("test@test.com")
                .name("tester")
                .provider(OAuth2Provider.GOOGLE)
                .role(UserRole.USER)
                .build();
        User savedUser = userRepository.save(testUser);

        CommunityPostEntity post = CommunityPostEntity.builder()
                .title("테스트 제목")
                .content("테스트 내용")
                .category("게시글 유형")
                .author(savedUser)
                .build();
        CommunityPostEntity savedPost = postRepository.save(post);

        // when: 조회수 증가
        int initialViewCount = savedPost.getViewCount();
        savedPost.increaseViewCount();
        postRepository.save(savedPost);

        // then: 조회수 증가 검증
        Optional<CommunityPostEntity> result = postRepository.findById(savedPost.getPostId());
        assertThat(result).isPresent();
        assertThat(result.get().getViewCount()).isEqualTo(initialViewCount + 1);
    }

    @Test
    @DisplayName("게시글 좋아요 수 증가/감소가 잘되는지 확인")
    void increaseLikeCount() {
        // given: 유저와 게시글 생성
        User testUser = User.builder()
                .email("test@test.com")
                .name("tester")
                .provider(OAuth2Provider.GOOGLE)
                .role(UserRole.USER)
                .build();
        User savedUser = userRepository.save(testUser);

        CommunityPostEntity post = CommunityPostEntity.builder()
                .title("테스트 제목")
                .content("테스트 내용")
                .category("게시글 유형")
                .author(savedUser)
                .build();
        CommunityPostEntity savedPost = postRepository.save(post);

        // when: 좋아요 수 증가
        int initialLikeCount = savedPost.getLikeCount();
        savedPost.increaseLikeCount();
        postRepository.save(savedPost);

        // then: 좋아요 수 증가 검증
        Optional<CommunityPostEntity> result = postRepository.findById(savedPost.getPostId());
        assertThat(result).isPresent();
        assertThat(result.get().getLikeCount()).isEqualTo(initialLikeCount + 1);

        // when: 좋아요 수 감소
        savedPost.decreaseLikeCount();
        postRepository.save(savedPost);

        // then: 좋아요 수 감소 검증
        result = postRepository.findById(savedPost.getPostId());
        assertThat(result).isPresent();
        assertThat(result.get().getLikeCount()).isEqualTo(initialLikeCount);
    }

    @Test
    @DisplayName("게시글 댓글 수 증가/감소가 잘되는지 확인")
    void increaseCommentCount() {
        // given: 유저와 게시글 생성
        User testUser = User.builder()
                .email("test@test.com")
                .name("tester")
                .provider(OAuth2Provider.GOOGLE)
                .role(UserRole.USER)
                .build();
        User savedUser = userRepository.save(testUser);

        CommunityPostEntity post = CommunityPostEntity.builder()
                .title("테스트 제목")
                .content("테스트 내용")
                .category("게시글 유형")
                .author(savedUser)
                .build();
        CommunityPostEntity savedPost = postRepository.save(post);

        // when: 댓글 수 증가
        int initialCommentCount = savedPost.getCommentCount();
        savedPost.increaseCommentCount();
        postRepository.save(savedPost);

        // then: 댓글 수 증가 검증
        Optional<CommunityPostEntity> result = postRepository.findById(savedPost.getPostId());
        assertThat(result).isPresent();
        assertThat(result.get().getCommentCount()).isEqualTo(initialCommentCount + 1);

        // when: 댓글 수 감소
        savedPost.decreaseCommentCount();
        postRepository.save(savedPost);

        // then: 댓글 수 감소 검증
        result = postRepository.findById(savedPost.getPostId());
        assertThat(result).isPresent();
        assertThat(result.get().getCommentCount()).isEqualTo(initialCommentCount);
    }

    @Test
    @DisplayName("게시글 이미지 저장이 잘되는지 확인")
    void saveImage() {
        // given: 유저와 게시글 생성
        User testUser = User.builder()
                .email("test@test.com")
                .name("tester")
                .provider(OAuth2Provider.GOOGLE)
                .role(UserRole.USER)
                .build();
        User savedUser = userRepository.save(testUser);

        CommunityPostEntity post = CommunityPostEntity.builder()
                .title("테스트 제목")
                .content("테스트 내용")
                .category("게시글 유형")
                .author(savedUser)
                .build();
        CommunityPostEntity savedPost = postRepository.save(post);

        // 이미지 생성
        String imageUrl = "https://example.com/image.jpg";
        Integer imageOrder = 1;

        CommunityImageEntity image = CommunityImageEntity.builder()
                .post(savedPost)
                .imageUrl(imageUrl)
                .imageOrder(imageOrder)
                .build();

        // when: 이미지 저장
        CommunityImageEntity savedImage = imageRepository.save(image);

        // then: 이미지 검증
        assertThat(savedImage.getImageId()).isNotNull();
        assertThat(savedImage.getImageUrl()).isEqualTo(imageUrl);
        assertThat(savedImage.getImageOrder()).isEqualTo(imageOrder);
        assertThat(savedImage.getPost().getPostId()).isEqualTo(savedPost.getPostId());
        assertThat(savedImage.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("게시글의 이미지 목록 조회가 잘되는지 확인")
    void findImagesByPost() {
        // given: 유저와 게시글 생성
        User testUser = User.builder()
                .email("test@test.com")
                .name("tester")
                .provider(OAuth2Provider.GOOGLE)
                .role(UserRole.USER)
                .build();
        User savedUser = userRepository.save(testUser);

        CommunityPostEntity post = CommunityPostEntity.builder()
                .title("테스트 제목")
                .content("테스트 내용")
                .category("게시글 유형")
                .author(savedUser)
                .build();
        CommunityPostEntity savedPost = postRepository.save(post);

        // 여러 이미지 생성
        CommunityImageEntity image1 = CommunityImageEntity.builder()
                .post(savedPost)
                .imageUrl("https://example.com/image1.jpg")
                .imageOrder(1)
                .build();
        CommunityImageEntity image2 = CommunityImageEntity.builder()
                .post(savedPost)
                .imageUrl("https://example.com/image2.jpg")
                .imageOrder(2)
                .build();
        CommunityImageEntity image3 = CommunityImageEntity.builder()
                .post(savedPost)
                .imageUrl("https://example.com/image3.jpg")
                .imageOrder(3)
                .build();

        imageRepository.save(image1);
        imageRepository.save(image2);
        imageRepository.save(image3);

        // when: 게시글의 이미지 목록 조회
        List<CommunityImageEntity> images = imageRepository.findByPostOrderByImageOrderAsc(savedPost);

        // then: 이미지 목록 검증
        assertThat(images).hasSize(3);
        assertThat(images.get(0).getImageOrder()).isEqualTo(1);
        assertThat(images.get(1).getImageOrder()).isEqualTo(2);
        assertThat(images.get(2).getImageOrder()).isEqualTo(3);
        assertThat(images).extracting(CommunityImageEntity::getImageUrl)
                .containsExactly(
                        "https://example.com/image1.jpg",
                        "https://example.com/image2.jpg",
                        "https://example.com/image3.jpg"
                );
    }

    @Test
    @DisplayName("게시글 이미지 삭제가 잘되는지 확인")
    void deleteImage() {
        // given: 유저, 게시글, 이미지 생성
        User testUser = User.builder()
                .email("test@test.com")
                .name("tester")
                .provider(OAuth2Provider.GOOGLE)
                .role(UserRole.USER)
                .build();
        User savedUser = userRepository.save(testUser);

        CommunityPostEntity post = CommunityPostEntity.builder()
                .title("테스트 제목")
                .content("테스트 내용")
                .category("게시글 유형")
                .author(savedUser)
                .build();
        CommunityPostEntity savedPost = postRepository.save(post);

        CommunityImageEntity image = CommunityImageEntity.builder()
                .post(savedPost)
                .imageUrl("https://example.com/image.jpg")
                .imageOrder(1)
                .build();
        CommunityImageEntity savedImage = imageRepository.save(image);

        // when: 이미지 삭제
        imageRepository.delete(savedImage);

        // then: 삭제 검증
        assertThat(imageRepository.findById(savedImage.getImageId())).isEmpty();
    }
}