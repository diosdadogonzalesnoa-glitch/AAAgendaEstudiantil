package com.darkcode.spring.app.service;

import com.darkcode.spring.app.model.Course;
import com.darkcode.spring.app.model.User;
import com.darkcode.spring.app.repository.CourseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CourseService {

    @Autowired
    private CourseRepository courseRepository;

    public List<Course> getCoursesByUser(User user) {
        return user.getCourses();
    }

    public void addCourse(Course course, User user) {

        // guardar en repo
        courseRepository.save(course);

        // 🔥 agregar al usuario (CLAVE)
        user.getCourses().add(course);
    }
}