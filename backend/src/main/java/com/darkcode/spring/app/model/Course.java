package com.darkcode.spring.app.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "courses")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String teacher;

    private String schedule;

    @Column(columnDefinition = "varchar(20) default 'ACTIVO'")
    private String status = "ACTIVO";

    // DOCENTE
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    // ESTUDIANTES INSCRITOS
    @JsonIgnore
    @ManyToMany(mappedBy = "enrolledCourses")
    private List<User> students = new ArrayList<>();

    // TAREAS
    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL)
    private List<Task> tasks = new ArrayList<>();

    // CLASES PUBLICADAS
    @JsonIgnore
    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Lesson> lessons = new ArrayList<>();

    public Course() {
    }

    public Course(Long id,
                  String name,
                  String teacher,
                  String schedule) {

        this.id = id;
        this.name = name;
        this.teacher = teacher;
        this.schedule = schedule;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getTeacher() {
        return teacher;
    }

    public String getSchedule() {
        return schedule;
    }

    public String getStatus() {
        return status == null ? "ACTIVO" : status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public User getUser() {
        return user;
    }

    public List<User> getStudents() {
        return students;
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public List<Lesson> getLessons() {
        return lessons;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setTeacher(String teacher) {
        this.teacher = teacher;
    }

    public void setSchedule(String schedule) {
        this.schedule = schedule;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setStudents(List<User> students) {
        this.students = students;
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks;
    }

    public void setLessons(List<Lesson> lessons) {
        this.lessons = lessons;
    }
}