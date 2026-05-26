package com.darkcode.spring.app.model;

import jakarta.persistence.*;

@Entity
@Table(name = "practice_options")
public class PracticeOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 1500)
    private String optionText;

    private int optionOrder;

    private boolean correct = false;

    @ManyToOne
    @JoinColumn(name = "question_id")
    private PracticeQuestion question;

    public PracticeOption() {}

    public Long getId() {
        return id;
    }

    public String getOptionText() {
        return optionText;
    }

    public int getOptionOrder() {
        return optionOrder;
    }

    public boolean isCorrect() {
        return correct;
    }

    public PracticeQuestion getQuestion() {
        return question;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setOptionText(String optionText) {
        this.optionText = optionText;
    }

    public void setOptionOrder(int optionOrder) {
        this.optionOrder = optionOrder;
    }

    public void setCorrect(boolean correct) {
        this.correct = correct;
    }

    public void setQuestion(PracticeQuestion question) {
        this.question = question;
    }
}