package com.darkcode.spring.app.model;

import jakarta.persistence.*;

@Entity
@Table(name = "practice_answers")
public class PracticeAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 5000)
    private String writtenAnswer;

    private Boolean correct;

    @ManyToOne
    @JoinColumn(name = "submission_id")
    private Submission submission;

    @ManyToOne
    @JoinColumn(name = "question_id")
    private PracticeQuestion question;

    @ManyToOne
    @JoinColumn(name = "selected_option_id")
    private PracticeOption selectedOption;

    public PracticeAnswer() {}

    public Long getId() {
        return id;
    }

    public String getWrittenAnswer() {
        return writtenAnswer;
    }

    public Boolean getCorrect() {
        return correct;
    }

    public Submission getSubmission() {
        return submission;
    }

    public PracticeQuestion getQuestion() {
        return question;
    }

    public PracticeOption getSelectedOption() {
        return selectedOption;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setWrittenAnswer(String writtenAnswer) {
        this.writtenAnswer = writtenAnswer;
    }

    public void setCorrect(Boolean correct) {
        this.correct = correct;
    }

    public void setSubmission(Submission submission) {
        this.submission = submission;
    }

    public void setQuestion(PracticeQuestion question) {
        this.question = question;
    }

    public void setSelectedOption(PracticeOption selectedOption) {
        this.selectedOption = selectedOption;
    }
}