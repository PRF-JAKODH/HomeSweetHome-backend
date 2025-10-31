package com.homesweet.homesweetback.domain.grade.repository;

import com.homesweet.homesweetback.domain.grade.entity.Grade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GradeRepository extends JpaRepository<Grade, Integer> {
    Grade findByGrade(String grade);
}
