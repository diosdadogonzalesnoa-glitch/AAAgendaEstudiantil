package com.darkcode.spring.app.repository;

import com.darkcode.spring.app.model.Course;
import com.darkcode.spring.app.model.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface LessonRepository extends JpaRepository<Lesson, Long> {

    List<Lesson> findByCourseOrderByLessonDateAscCreatedAtAsc(Course course);

    List<Lesson> findByCourseAndLessonDateOrderByCreatedAtAsc(Course course, LocalDate lessonDate);
}