package com.darkcode.spring.app.repository;

import com.darkcode.spring.app.model.PracticeOption;
import com.darkcode.spring.app.model.PracticeQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PracticeOptionRepository extends JpaRepository<PracticeOption, Long> {

    List<PracticeOption> findByQuestionOrderByOptionOrderAsc(PracticeQuestion question);
}