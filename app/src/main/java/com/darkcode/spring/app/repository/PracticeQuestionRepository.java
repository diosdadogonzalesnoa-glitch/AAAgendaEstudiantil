package com.darkcode.spring.app.repository;

import com.darkcode.spring.app.model.PracticeQuestion;
import com.darkcode.spring.app.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PracticeQuestionRepository extends JpaRepository<PracticeQuestion, Long> {

    List<PracticeQuestion> findByTask(Task task);
}