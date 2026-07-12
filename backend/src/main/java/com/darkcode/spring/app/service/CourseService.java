package com.darkcode.spring.app.service;

import com.darkcode.spring.app.model.Course;
import com.darkcode.spring.app.model.User;
import com.darkcode.spring.app.repository.CourseRepository;
import com.darkcode.spring.app.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CourseService {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    // CURSOS DEL DOCENTE
    public List<Course> getCoursesByUser(User user) {
        return courseRepository.findByUserId(user.getId());
    }

    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    public List<Course> searchCourses(String name) {
        return courseRepository.findByNameContainingIgnoreCase(name);
    }

    @Transactional
    public Course getCourseById(Long id) {
        return courseRepository.findById(id).orElse(null);
    }

    public void addCourse(Course course, User user) {

        User teacher = userRepository
                .findById(user.getId())
                .orElse(null);

        if (teacher != null) {
            course.setUser(teacher);
            courseRepository.save(course);
        }
    }

    @Transactional
    public List<Course> getEnrolledCoursesByStudent(User sessionUser) {

        User student = userRepository
                .findById(sessionUser.getId())
                .orElse(null);

        if (student == null) {
            return new ArrayList<>();
        }

        return new ArrayList<>(student.getEnrolledCourses());
    }

    @Transactional
    public void enrollStudent(User sessionUser, Long courseId) {

        User student = userRepository
                .findById(sessionUser.getId())
                .orElse(null);

        Course course = courseRepository
                .findById(courseId)
                .orElse(null);

        if (student != null && course != null) {

            boolean alreadyEnrolled = student
                    .getEnrolledCourses()
                    .stream()
                    .anyMatch(c -> c.getId().equals(courseId));

            if (!alreadyEnrolled) {
                student.getEnrolledCourses().add(course);
                userRepository.save(student);
            }
        }
    }

    public void updateCourse(Long id,
                             String name,
                             String teacher,
                             String schedule) {

        Course course = courseRepository.findById(id).orElse(null);

        if (course != null) {
            course.setName(name);
            course.setTeacher(teacher);
            course.setSchedule(schedule);
            courseRepository.save(course);
        }
    }

    public void updateCourseStatus(Long id, String status) {
        Course course = courseRepository.findById(id).orElse(null);
        if (course != null) {
            course.setStatus(status);
            courseRepository.save(course);
        }
    }

    @Transactional
    public void deleteCourse(Long id, User user) {

        Course course = courseRepository.findById(id).orElse(null);

        if (course != null &&
            course.getUser() != null &&
            course.getUser().getId().equals(user.getId())) {

            List<User> users = userRepository.findAll();

            for (User u : users) {
                u.getEnrolledCourses().removeIf(c -> c.getId().equals(id));
                userRepository.save(u);
            }

            courseRepository.delete(course);
        }
    }
}