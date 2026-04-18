package com.darkcode.spring.app.service;

import com.darkcode.spring.app.model.StudySession;
import com.darkcode.spring.app.repository.StudySessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StudyService {

    @Autowired
    private StudySessionRepository repo;

    public StudySession save(StudySession s) {
        return repo.save(s);
    }

    public List<StudySession> list() {
        return repo.findAll();
    }
}