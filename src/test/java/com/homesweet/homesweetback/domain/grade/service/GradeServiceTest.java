package com.homesweet.homesweetback.domain.grade.service;

import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.entity.UserRole;
import com.homesweet.homesweetback.domain.grade.entity.Grade;
import com.homesweet.homesweetback.domain.grade.repository.GradeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("등급 단위 테스트")
class GradeServiceTest {

    @InjectMocks
    private GradeService gradeService;

    @Mock
    private GradeRepository gradeRepository;

    private User createUser(UserRole role, BigDecimal feeRate) {
        Grade grade = Grade.builder()
                .feeRate(feeRate)
                .build();

        return User.builder()
                .role(role)
                .grade(grade)
                .build();
    }

    @Test
    @DisplayName("[성공] 판매자이고 수수료율이 있으면 정상 계산")
    void calculateFee_success_seller_with_rate() {
        User user = createUser(UserRole.SELLER, BigDecimal.valueOf(0.10));
        BigDecimal salesAmount = BigDecimal.valueOf(10000);

        BigDecimal result = gradeService.calculateFeeforUser(salesAmount, user);

        assertThat(result).isEqualTo(BigDecimal.valueOf(1000.00).setScale(2));
    }

    @Test
    @DisplayName("[성공] 판매자가 아니면 수수료는 0원")
    void calculateFee_success_not_seller() {
        User user = createUser(UserRole.USER, BigDecimal.valueOf(0.1));
        BigDecimal salesAmount = BigDecimal.valueOf(10000);

        BigDecimal result = gradeService.calculateFeeforUser(salesAmount, user);

        assertThat(result).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("[실패] salesAmount 가 null → NPE 발생")
    void calculateFee_fail_salesAmount_null() {
        User user = createUser(UserRole.SELLER, BigDecimal.valueOf(0.1));

        assertThatThrownBy(() ->
                gradeService.calculateFeeforUser(null, user)
        ).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("[실패] user 가 null → NPE 발생")
    void calculateFee_fail_user_null() {

        assertThatThrownBy(() ->
                gradeService.calculateFeeforUser(BigDecimal.valueOf(10000), null)
        ).isInstanceOf(NullPointerException.class);
    }
}