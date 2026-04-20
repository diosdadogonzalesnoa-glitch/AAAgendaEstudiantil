package com.darkcode.spring.app.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Course {

    private String name;
    private String teacher;
    private String schedule;

    @JsonIgnore // 🔥 EVITA EL LOOP INFINITO
    private User user;

    public Course() {}

    public Course(String name, String teacher, String schedule) {
        this.name = name;
        this.teacher = teacher;
        this.schedule = schedule;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTeacher() { return teacher; }
    public void setTeacher(String teacher) { this.teacher = teacher; }

    public String getSchedule() { return schedule; }
    public void setSchedule(String schedule) { this.schedule = schedule; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}