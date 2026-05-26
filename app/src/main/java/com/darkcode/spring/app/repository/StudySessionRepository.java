package com.darkcode.spring.app.repository;

import com.darkcode.spring.app.model.StudySession;
import com.darkcode.spring.app.model.Task;
import com.darkcode.spring.app.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudySessionRepository extends JpaRepository<StudySession, Long> {

    List<StudySession> findByStudent(User student);

    List<StudySession> findByTask(Task task);

    void deleteByTask(Task task);
}