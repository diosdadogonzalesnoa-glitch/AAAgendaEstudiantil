package com.darkcode.spring.app.model;

import java.util.List;

public class LessonGroup {

    private String title;

    private List<Lesson> lessons;

    public LessonGroup() {
    }

    public LessonGroup(String title, List<Lesson> lessons) {
        this.title = title;
        this.lessons = lessons;
    }

    public String getTitle() {
        return title;
    }

    public List<Lesson> getLessons() {
        return lessons;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setLessons(List<Lesson> lessons) {
        this.lessons = lessons;
    }
}