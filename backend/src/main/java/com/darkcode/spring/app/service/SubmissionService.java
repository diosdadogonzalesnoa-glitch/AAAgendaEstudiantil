package com.darkcode.spring.app.service;

import com.darkcode.spring.app.model.Course;
import com.darkcode.spring.app.model.PracticeAnswer;
import com.darkcode.spring.app.model.PracticeOption;
import com.darkcode.spring.app.model.PracticeQuestion;
import com.darkcode.spring.app.model.Submission;
import com.darkcode.spring.app.model.Task;
import com.darkcode.spring.app.model.User;
import com.darkcode.spring.app.repository.PracticeAnswerRepository;
import com.darkcode.spring.app.repository.PracticeOptionRepository;
import com.darkcode.spring.app.repository.PracticeQuestionRepository;
import com.darkcode.spring.app.repository.SubmissionRepository;
import com.darkcode.spring.app.repository.TaskRepository;
import com.darkcode.spring.app.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SubmissionService {

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PracticeQuestionRepository practiceQuestionRepository;

    @Autowired
    private PracticeOptionRepository practiceOptionRepository;

    @Autowired
    private PracticeAnswerRepository practiceAnswerRepository;

    private final Path uploadPath = Paths.get("uploads", "submissions");

    @Transactional
    public void submitTask(Long taskId,
                           User sessionUser,
                           String answerText,
                           MultipartFile[] files) {

        Task task = taskRepository.findById(taskId).orElse(null);
        User student = userRepository.findById(sessionUser.getId()).orElse(null);

        if (task == null || student == null) {
            return;
        }

        boolean hasText = answerText != null && !answerText.trim().isEmpty();

        List<MultipartFile> validFiles = new ArrayList<>();

        if (files != null) {
            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    validFiles.add(file);
                }
            }
        }

        if (!hasText && validFiles.isEmpty()) {
            throw new RuntimeException("Debes escribir una respuesta o subir al menos un archivo.");
        }

        int fileLimit = task.getMaxFiles();
        if (validFiles.size() > fileLimit) {
            throw new RuntimeException("Solo puedes subir como máximo " + fileLimit + " archivos.");
        }

        Submission submission = submissionRepository.findByTaskAndStudent(task, student);

        if (submission == null) {
            submission = new Submission();
            submission.setTask(task);
            submission.setStudent(student);
            submission.setAttemptNumber(1);
        } else {
            int maxAttempts = task.getMaxAttempts();
            if (submission.getAttemptNumber() >= maxAttempts) {
                throw new RuntimeException("Ya utilizaste todos tus intentos para esta tarea.");
            }
            submission.setAttemptNumber(submission.getAttemptNumber() + 1);
        }

        submission.setAnswerText(answerText);
        submission.setSubmittedAt(LocalDateTime.now());

        List<String> savedFiles = new ArrayList<>();

        try {
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            for (MultipartFile file : validFiles) {

                String originalName = file.getOriginalFilename();

                if (originalName == null || originalName.trim().isEmpty()) {
                    originalName = "archivo";
                }

                originalName = Paths.get(originalName).getFileName().toString();

                String storedName = UUID.randomUUID() + "_" + originalName;

                Path filePath = uploadPath.resolve(storedName).normalize();

                Files.copy(
                        file.getInputStream(),
                        filePath,
                        StandardCopyOption.REPLACE_EXISTING
                );

                savedFiles.add(originalName + "||" + storedName);
            }

        } catch (Exception e) {
            throw new RuntimeException("Error al subir archivos: " + e.getMessage());
        }

        if (!savedFiles.isEmpty()) {
            submission.setFileNames(String.join(";;", savedFiles));
        }

        submissionRepository.save(submission);
    }

    @Transactional
    public void submitPracticeTask(Long taskId,
                                   User sessionUser,
                                   Map<String, String> answers) {

        Task task = taskRepository.findById(taskId).orElse(null);
        User student = userRepository.findById(sessionUser.getId()).orElse(null);

        if (task == null || student == null) {
            return;
        }

        Submission submission = submissionRepository.findByTaskAndStudent(task, student);

        if (submission == null) {
            submission = new Submission();
            submission.setTask(task);
            submission.setStudent(student);
        }

        submission.setSubmittedAt(LocalDateTime.now());

        Submission savedSubmission = submissionRepository.save(submission);

        List<PracticeAnswer> oldAnswers =
                practiceAnswerRepository.findBySubmission(savedSubmission);

        practiceAnswerRepository.deleteAll(oldAnswers);

        List<PracticeQuestion> questions =
                practiceQuestionRepository.findByTask(task)
                        .stream()
                        .sorted(Comparator.comparing(PracticeQuestion::getQuestionOrder))
                        .collect(Collectors.toList());

        int automaticGrade = 0;
        boolean hasWrittenQuestions = false;

        StringBuilder writtenSummary = new StringBuilder();

        for (PracticeQuestion question : questions) {

            String key = "answer_" + question.getId();
            String value = answers.get(key);

            PracticeAnswer answer = new PracticeAnswer();
            answer.setSubmission(savedSubmission);
            answer.setQuestion(question);

            if (question.isWrittenQuestion()) {

                hasWrittenQuestions = true;

                answer.setWrittenAnswer(value);
                answer.setCorrect(null);

                writtenSummary.append("Pregunta ")
                        .append(question.getQuestionOrder())
                        .append(": ")
                        .append(question.getQuestionText())
                        .append("\n");

                writtenSummary.append("Respuesta escrita: ")
                        .append(value == null ? "" : value)
                        .append("\n\n");

            } else {

                Long selectedOptionId = null;

                try {
                    if (value != null && !value.trim().isEmpty()) {
                        selectedOptionId = Long.parseLong(value);
                    }
                } catch (Exception ignored) {
                }

                PracticeOption selectedOption = null;

                if (selectedOptionId != null) {
                    selectedOption =
                            practiceOptionRepository.findById(selectedOptionId)
                                    .orElse(null);
                }

                answer.setSelectedOption(selectedOption);

                boolean isCorrect =
                        selectedOption != null && selectedOption.isCorrect();

                answer.setCorrect(isCorrect);

                if (isCorrect) {
                    automaticGrade += question.getPoints();
                }
            }

            practiceAnswerRepository.save(answer);
        }

        if (hasWrittenQuestions) {
            savedSubmission.setAnswerText(writtenSummary.toString());
            savedSubmission.setGrade(null);
        } else {
            savedSubmission.setAnswerText(null);
            savedSubmission.setGrade(Math.min(automaticGrade, 20));
        }

        submissionRepository.save(savedSubmission);
    }

    @Transactional
    public Long gradeSubmission(Long submissionId, Integer grade) {

        Submission submission =
                submissionRepository.findById(submissionId)
                        .orElse(null);

        if (submission == null) {
            return null;
        }

        if (grade == null || grade < 0 || grade > 20) {
            return submission.getTask() != null ? submission.getTask().getId() : null;
        }

        submission.setGrade(grade);

        submissionRepository.save(submission);

        return submission.getTask() != null ? submission.getTask().getId() : null;
    }

    @Transactional
    public List<Submission> getSubmissionsByTask(Long taskId) {

        Task task = taskRepository.findById(taskId).orElse(null);

        if (task == null) {
            return new ArrayList<>();
        }

        return submissionRepository.findByTask(task)
                .stream()
                .sorted(Comparator.comparing(s -> s.getStudent().getName()))
                .collect(Collectors.toList());
    }

    @Transactional
    public List<User> getPendingStudentsByTask(Long taskId) {

        Task task = taskRepository.findById(taskId).orElse(null);

        if (task == null || task.getCourse() == null) {
            return new ArrayList<>();
        }

        Course course = task.getCourse();

        List<User> students = new ArrayList<>(course.getStudents());

        List<Submission> submissions = submissionRepository.findByTask(task);

        Set<Long> submittedIds = submissions
                .stream()
                .map(s -> s.getStudent().getId())
                .collect(Collectors.toSet());

        return students
                .stream()
                .filter(student -> !submittedIds.contains(student.getId()))
                .sorted(Comparator.comparing(User::getName))
                .collect(Collectors.toList());
    }

    @Transactional
    public Set<Long> getSubmittedTaskIds(User sessionUser) {

        User student = userRepository.findById(sessionUser.getId()).orElse(null);

        if (student == null) {
            return new HashSet<>();
        }

        return submissionRepository.findByStudent(student)
                .stream()
                .map(s -> s.getTask().getId())
                .collect(Collectors.toSet());
    }

    @Transactional
    public Map<Long, Submission> getSubmissionsByStudentMappedByTask(User sessionUser) {

        User student = userRepository.findById(sessionUser.getId()).orElse(null);

        if (student == null) {
            return new HashMap<>();
        }

        return submissionRepository.findByStudent(student)
                .stream()
                .filter(s -> s.getTask() != null)
                .collect(Collectors.toMap(
                        s -> s.getTask().getId(),
                        s -> s,
                        (a, b) -> a
                ));
    }

    @Transactional
    public Submission getSubmissionByTaskAndStudent(Task task, User student) {

        if (task == null || student == null) {
            return null;
        }

        User realStudent =
                userRepository.findById(student.getId()).orElse(null);

        if (realStudent == null) {
            return null;
        }

        return submissionRepository.findByTaskAndStudent(task, realStudent);
    }

    @Transactional
    public Map<Long, List<PracticeAnswer>> getPracticeAnswersByTask(Long taskId) {

        Map<Long, List<PracticeAnswer>> result = new HashMap<>();

        List<Submission> submissions = getSubmissionsByTask(taskId);

        for (Submission submission : submissions) {
            result.put(
                    submission.getId(),
                    practiceAnswerRepository.findBySubmission(submission)
            );
        }

        return result;
    }

    public List<PracticeAnswer> getPracticeAnswersBySubmission(Submission submission) {
        return practiceAnswerRepository.findBySubmission(submission);
    }

    @Transactional
    public int markAnswerCorrect(Long answerId, boolean correct) {
        PracticeAnswer answer = practiceAnswerRepository.findById(answerId).orElse(null);
        if (answer == null) return -1;
        answer.setCorrect(correct);
        practiceAnswerRepository.save(answer);

        Submission submission = answer.getSubmission();
        List<PracticeAnswer> all = practiceAnswerRepository.findBySubmission(submission);
        int total = all.stream()
            .filter(a -> Boolean.TRUE.equals(a.getCorrect()))
            .mapToInt(a -> a.getQuestion() != null ? a.getQuestion().getPoints() : 0)
            .sum();
        int grade = Math.min(20, total);
        submission.setGrade(grade);
        submissionRepository.save(submission);
        return grade;
    }

    @Transactional
    public void addFeedback(Long submissionId, String feedback) {
        Submission submission = submissionRepository.findById(submissionId).orElse(null);
        if (submission == null) return;
        submission.setFeedback(feedback);
        submissionRepository.save(submission);
    }

    @Transactional
    public Map<Long, List<Map<String, Object>>> getSerializedPracticeAnswersByTask(Long taskId) {
        Map<Long, List<Map<String, Object>>> result = new HashMap<>();
        List<Submission> submissions = getSubmissionsByTask(taskId);
        for (Submission submission : submissions) {
            List<PracticeAnswer> answers = practiceAnswerRepository.findBySubmission(submission);
            List<Map<String, Object>> answerList = answers.stream().map(a -> {
                Map<String, Object> am = new HashMap<>();
                am.put("id", a.getId());
                am.put("writtenAnswer", a.getWrittenAnswer());
                am.put("correct", a.getCorrect());
                if (a.getQuestion() != null) {
                    Map<String, Object> qm = new HashMap<>();
                    qm.put("questionOrder", a.getQuestion().getQuestionOrder());
                    qm.put("questionText", a.getQuestion().getQuestionText());
                    qm.put("writtenQuestion", a.getQuestion().isWrittenQuestion());
                    am.put("question", qm);
                }
                if (a.getSelectedOption() != null) {
                    am.put("selectedOption", Map.of("optionText", a.getSelectedOption().getOptionText()));
                }
                return am;
            }).toList();
            result.put(submission.getId(), answerList);
        }
        return result;
    }

    public Path getUploadPath() {
        return uploadPath;
    }

    public List<Map<String, String>> getFilesFromSubmission(Submission submission) {

        List<Map<String, String>> files = new ArrayList<>();

        if (submission.getFileNames() == null || submission.getFileNames().isEmpty()) {
            return files;
        }

        String[] filePairs = submission.getFileNames().split(";;");

        for (String pair : filePairs) {
            String[] parts = pair.split("\\Q||\\E");

            if (parts.length == 2) {
                Map<String, String> file = new HashMap<>();
                file.put("originalName", parts[0]);
                file.put("storedName", parts[1]);
                files.add(file);
            }
        }

        return files;
    }
}