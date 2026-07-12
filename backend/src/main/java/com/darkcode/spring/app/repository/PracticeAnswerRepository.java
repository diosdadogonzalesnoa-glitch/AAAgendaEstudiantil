package com.darkcode.spring.app.repository;

import com.darkcode.spring.app.model.PracticeAnswer;
import com.darkcode.spring.app.model.Submission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PracticeAnswerRepository extends JpaRepository<PracticeAnswer, Long> {

    List<PracticeAnswer> findBySubmission(Submission submission);
}