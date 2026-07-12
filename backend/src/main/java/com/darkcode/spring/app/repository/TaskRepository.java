package com.darkcode.spring.app.repository;

import com.darkcode.spring.app.model.Course;
import com.darkcode.spring.app.model.Task;
import com.darkcode.spring.app.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByUser(User user);

    List<Task> findByCourse(Course course);

}