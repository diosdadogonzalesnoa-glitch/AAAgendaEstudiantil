package com.darkcode.spring.app.repository;

import com.darkcode.spring.app.model.StudySession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudySessionRepository extends JpaRepository<StudySession, Long> {
}