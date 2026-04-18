package com.darkcode.spring.app.service;

import com.darkcode.spring.app.model.Course;
import com.darkcode.spring.app.repository.CourseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CourseService {

    @Autowired
    private CourseRepository repo;

    public Course save(Course course) {
        return repo.save(course);
    }

    public List<Course> list() {
        return repo.findAll();
    }
}