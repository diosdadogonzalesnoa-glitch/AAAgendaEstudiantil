package com.darkcode.spring.app.repository;

import com.darkcode.spring.app.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, Long> {
}