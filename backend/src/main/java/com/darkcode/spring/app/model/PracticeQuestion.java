package com.darkcode.spring.app.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "practice_questions")
public class PracticeQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 3000)
    private String questionText;

    private int points = 4;

    private int questionOrder;

    private boolean writtenQuestion = false;

    @ManyToOne
    @JoinColumn(name = "task_id")
    private Task task;

    @JsonIgnore
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PracticeOption> options = new ArrayList<>();

    public PracticeQuestion() {}

    public Long getId() {
        return id;
    }

    public String getQuestionText() {
        return questionText;
    }

    public int getPoints() {
        return points;
    }

    public int getQuestionOrder() {
        return questionOrder;
    }

    public boolean isWrittenQuestion() {
        return writtenQuestion;
    }

    public Task getTask() {
        return task;
    }

    public List<PracticeOption> getOptions() {
        return options;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public void setQuestionOrder(int questionOrder) {
        this.questionOrder = questionOrder;
    }

    public void setWrittenQuestion(boolean writtenQuestion) {
        this.writtenQuestion = writtenQuestion;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public void setOptions(List<PracticeOption> options) {
        this.options = options;
    }
}