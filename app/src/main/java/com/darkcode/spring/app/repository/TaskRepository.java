package com.darkcode.spring.app.repository;

import com.darkcode.spring.app.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {
}