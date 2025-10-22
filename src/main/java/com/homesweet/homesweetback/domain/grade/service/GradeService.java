package com.homesweet.homesweetback.domain.grade.service;


import com.homesweet.homesweetback.domain.grade.entity.Grade;
import com.homesweet.homesweetback.domain.grade.repository.GradeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GradeService {

    private final GradeRepository gradeRepository;

    // 모든 등급을 조회
    public List<Grade> getAllGrade() {
        return gradeRepository.findAll();
    }

    // 수수료 계산

}
