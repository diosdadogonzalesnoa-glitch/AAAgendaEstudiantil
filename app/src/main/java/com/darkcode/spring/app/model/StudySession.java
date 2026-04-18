package com.darkcode.spring.app.model;

import jakarta.persistence.*;

@Entity
public class StudySession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int minutes;

    public StudySession() {}

    public Long getId() { return id; }

    public int getMinutes() { return minutes; }
    public void setMinutes(int minutes) { this.minutes = minutes; }
}