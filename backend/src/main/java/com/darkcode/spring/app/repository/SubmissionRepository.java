package com.darkcode.spring.app.repository;

import com.darkcode.spring.app.model.Submission;
import com.darkcode.spring.app.model.Task;
import com.darkcode.spring.app.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    List<Submission> findByTask(Task task);

    List<Submission> findByStudent(User student);

    Submission findByTaskAndStudent(Task task, User student);
}