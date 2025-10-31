package com.homesweet.homesweetback.domain.grade.service;


import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.entity.UserRole;
import com.homesweet.homesweetback.domain.grade.repository.GradeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class GradeService {

    private final GradeRepository gradeRepository;

    // 등급에 따른 수수료 계산
    public BigDecimal calculateFeeforUser(BigDecimal salesAmount, User user){
        // 판매자가 아니라면 0
        if(user.getRole() != UserRole.SELLER){
            return BigDecimal.ZERO;
        }
        // 등급별 수수료율
        BigDecimal feeRate = user.getGrade().getFeeRate();
        return salesAmount.multiply(feeRate);
    }
}
