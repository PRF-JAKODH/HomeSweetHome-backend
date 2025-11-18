package com.homesweet.homesweetback.domain.grade.service;

import com.homesweet.homesweetback.domain.auth.entity.OAuth2Provider;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.entity.UserRole;
import com.homesweet.homesweetback.domain.auth.repository.UserRepository;
import com.homesweet.homesweetback.domain.grade.entity.Grade;
import com.homesweet.homesweetback.domain.grade.repository.GradeRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class GradeServiceIntegrationTest {
    @Autowired
    private GradeService gradeService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GradeRepository gradeRepository;

    @Autowired
    private EntityManager em;

    private User seller;
    private Grade vip;

    @BeforeEach
    void setup() {
        // Grade 저장
        vip = Grade.builder()
                .grade("VIP")
                .feeRate(new BigDecimal("0.25")) // 10%
                .build();

        gradeRepository.save(vip);

        // 판매자 저장
        seller = User.builder()
                .name("seller1")
                .email("test@test.com")
                .provider(OAuth2Provider.GOOGLE)
                .role(UserRole.SELLER)
                .grade(vip)
                .build();

        userRepository.save(seller);

        em.flush();
        em.clear();
    }

    @Nested
    @DisplayName("성공 케이스")
    class Success {
        @Test
        @DisplayName("판매자 + grade 존재 → 정상 수수료 계산")
        void calculateFee_success() {
            BigDecimal salesAmount = new BigDecimal("100000");

            BigDecimal fee = gradeService.calculateFeeforUser(salesAmount, seller);

            assertThat(fee).isEqualByComparingTo("25000.00");  // 10%
        }
    }

    @Nested
    @DisplayName("실패 케이스")
    class Fail {
        @Test
        @DisplayName("판매자가 아니면 수수료 0원")
        void notSeller_feeZero() {
            User buyer = User.builder()
                    .name("buyer1")
                    .provider(OAuth2Provider.GOOGLE)
                    .email("t4@test.com")
                    .role(UserRole.USER)
                    .grade(vip)
                    .build();

            userRepository.save(buyer);

            BigDecimal fee = gradeService.calculateFeeforUser(new BigDecimal("200000"), buyer);

            assertThat(fee).isEqualByComparingTo("0.00");
        }
    }
}
