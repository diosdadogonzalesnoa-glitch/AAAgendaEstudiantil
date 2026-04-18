package com.darkcode.spring.app.service;

import com.darkcode.spring.app.model.StudySession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StatisticsService {

    @Autowired
    private StudyService studyService;

    public int totalMinutes() {
        List<StudySession> list = studyService.list();

        return list.stream()
                .mapToInt(StudySession::getMinutes)
                .sum();
    }
}