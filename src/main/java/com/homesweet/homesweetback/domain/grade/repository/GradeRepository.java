package com.homesweet.homesweetback.domain.grade.repository;

import com.homesweet.homesweetback.domain.grade.entity.Grade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GradeRepository extends JpaRepository<Grade, Integer> {
    // 등급 정보는 등급명으로 조회  
    Grade findByGrade(String grade);

}
