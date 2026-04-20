package com.darkcode.spring.app.repository;

import com.darkcode.spring.app.model.Course;
import com.darkcode.spring.app.model.User;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class CourseRepository {

    private List<Course> courses = new ArrayList<>();

    public List<Course> findAll() {
        return courses;
    }

    // 🔥 FILTRAR POR USUARIO
    public List<Course> findByUser(User user) {
        List<Course> result = new ArrayList<>();

        for (Course c : courses) {
            if (c.getUser() != null && c.getUser().getId().equals(user.getId())) {
                result.add(c);
            }
        }

        return result;
    }

    public void save(Course course) {
        courses.add(course);
    }
}