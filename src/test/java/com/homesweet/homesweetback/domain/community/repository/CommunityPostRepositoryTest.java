package com.homesweet.homesweetback.domain.community.repository;

import com.homesweet.homesweetback.domain.auth.entity.OAuth2Provider;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.entity.UserRole;
import com.homesweet.homesweetback.domain.auth.repository.UserRepository;
import com.homesweet.homesweetback.domain.community.entity.CommunityPostEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// @DataJpaTest
@SpringBootTest
@Transactional
class CommunityPostRepositoryTest {

    @Autowired
    private CommunityPostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

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
//    잘 안됨
//    @Test
//    @DisplayName("모든게시글 조회 확인")
//    @Transactional
//    void findByPostIdAndIsDeletedFalse() {
//        // given: 테스트 데이터
//
//        // user 먼저 생성
//        User testUser = User.builder()
//                .email("test@test.com")
//                .name("tester")
//                .provider(OAuth2Provider.GOOGLE)
//                .role(UserRole.USER)
//                .build();
//        User savedUser = userRepository.save(testUser);
//
//        // post 생성
//        CommunityPostEntity post1 = CommunityPostEntity.builder()
//                .title("title")
//                .content("content")
//                .category("category")
//                .author(savedUser)
//                .build();
//
//        CommunityPostEntity post2 = CommunityPostEntity.builder()
//                .title("title2")
//                .content("content2")
//                .category("category2")
//                .author(savedUser)
//                .build();
//
//
//        postRepository.save(post1);
//        postRepository.save(post2);
//
//
//        // when
//        List<CommunityPostEntity> posts = postRepository.findByPostIdAndIsDeletedFalse();
//
//        // then
//        assertThat(posts).isNotNull();
//        assertThat(posts.size()).isEqualTo(2);
//        assertThat(posts).extracting(CommunityPostEntity::getTitle)
//                .containsExactlyInAnyOrder("title", "title2");
//    }
}

