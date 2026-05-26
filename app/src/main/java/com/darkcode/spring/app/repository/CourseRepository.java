package com.darkcode.spring.app.repository;

import com.darkcode.spring.app.model.Course;
import com.darkcode.spring.app.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {

    // CURSOS DEL DOCENTE
    List<Course> findByUser(User user);

    // CURSOS DEL DOCENTE POR ID
    List<Course> findByUserId(Long userId);

    // BUSCADOR DE CURSOS
    List<Course> findByNameContainingIgnoreCase(String name);

}