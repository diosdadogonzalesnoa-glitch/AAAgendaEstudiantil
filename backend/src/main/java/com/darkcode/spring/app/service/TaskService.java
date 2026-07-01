package com.darkcode.spring.app.service;

import com.darkcode.spring.app.model.Course;
import com.darkcode.spring.app.model.PracticeOption;
import com.darkcode.spring.app.model.PracticeQuestion;
import com.darkcode.spring.app.model.Submission;
import com.darkcode.spring.app.model.Task;
import com.darkcode.spring.app.model.User;
import com.darkcode.spring.app.repository.CourseRepository;
import com.darkcode.spring.app.repository.PracticeOptionRepository;
import com.darkcode.spring.app.repository.PracticeQuestionRepository;
import com.darkcode.spring.app.repository.StudySessionRepository;
import com.darkcode.spring.app.repository.SubmissionRepository;
import com.darkcode.spring.app.repository.TaskRepository;
import com.darkcode.spring.app.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TaskService {

    @Autowired
    private TaskRepository repo;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private PracticeQuestionRepository practiceQuestionRepository;

    @Autowired
    private PracticeOptionRepository practiceOptionRepository;

    @Autowired
    private StudySessionRepository studySessionRepository;

    public Task save(Task task) {
        return repo.save(task);
    }

    public Task getTaskById(Long id) {
        return repo.findById(id).orElse(null);
    }

    public void addTask(Task task,
                        Long courseId,
                        User user) {

        Course course =
                courseRepository.findById(courseId)
                        .orElse(null);

        User teacher =
                userRepository.findById(user.getId())
                        .orElse(null);

        if (course != null && teacher != null) {

            task.setCourse(course);
            task.setUser(teacher);

            if (task.getTaskType() == null || task.getTaskType().trim().isEmpty()) {
                task.setTaskType("TAREA");
            }

            repo.save(task);
        }
    }

    @Transactional
    public void addPracticeTask(Task task,
                                Long courseId,
                                User user,
                                List<String> questionTexts,
                                List<String> option1,
                                List<String> option2,
                                List<String> option3,
                                List<String> option4,
                                List<String> option5,
                                List<Integer> correctOptions) {

        Course course =
                courseRepository.findById(courseId)
                        .orElse(null);

        User teacher =
                userRepository.findById(user.getId())
                        .orElse(null);

        if (course == null || teacher == null) {
            return;
        }

        if (questionTexts == null) {
            throw new RuntimeException("La práctica debe tener 5, 10 o 20 enunciados.");
        }

        List<String> validQuestions = questionTexts
                .stream()
                .filter(q -> q != null && !q.trim().isEmpty())
                .collect(Collectors.toList());

        int totalQuestions = validQuestions.size();

        if (totalQuestions != 5 && totalQuestions != 10 && totalQuestions != 20) {
            throw new RuntimeException("Solo puedes publicar prácticas con 5, 10 o 20 enunciados.");
        }

        int pointsPerQuestion;

        if (totalQuestions == 5) {
            pointsPerQuestion = 4;
        } else if (totalQuestions == 10) {
            pointsPerQuestion = 2;
        } else {
            pointsPerQuestion = 1;
        }

        task.setCourse(course);
        task.setUser(teacher);
        task.setTaskType("PRACTICA");

        if (task.getPriority() == null || task.getPriority().trim().isEmpty()) {
            task.setPriority("MEDIA");
        }

        Task savedTask = repo.save(task);

        int questionOrder = 1;

        for (int i = 0; i < questionTexts.size(); i++) {

            String text = questionTexts.get(i);

            if (text == null || text.trim().isEmpty()) {
                continue;
            }

            PracticeQuestion question = new PracticeQuestion();
            question.setTask(savedTask);
            question.setQuestionText(text.trim());
            question.setPoints(pointsPerQuestion);
            question.setQuestionOrder(questionOrder);

            PracticeQuestion savedQuestion =
                    practiceQuestionRepository.save(question);

            List<String> options = new ArrayList<>();

            options.add(getValue(option1, i));
            options.add(getValue(option2, i));
            options.add(getValue(option3, i));
            options.add(getValue(option4, i));
            options.add(getValue(option5, i));

            Integer correctOption =
                    getCorrectOption(correctOptions, i);

            boolean hasOptions = false;

            for (String optionText : options) {
                if (optionText != null && !optionText.trim().isEmpty()) {
                    hasOptions = true;
                    break;
                }
            }

            if (!hasOptions) {
                savedQuestion.setWrittenQuestion(true);
                practiceQuestionRepository.save(savedQuestion);
                questionOrder++;
                continue;
            }

            if (correctOption == null || correctOption < 1 || correctOption > 5) {
                throw new RuntimeException("Cada pregunta con alternativas debe tener una respuesta correcta marcada.");
            }

            savedQuestion.setWrittenQuestion(false);
            practiceQuestionRepository.save(savedQuestion);

            int optionNumber = 1;

            for (String optionText : options) {

                if (optionText != null && !optionText.trim().isEmpty()) {

                    PracticeOption option = new PracticeOption();
                    option.setQuestion(savedQuestion);
                    option.setOptionText(optionText.trim());
                    option.setOptionOrder(optionNumber);
                    option.setCorrect(correctOption.equals(optionNumber));

                    practiceOptionRepository.save(option);
                }

                optionNumber++;
            }

            questionOrder++;
        }
    }

    private String getValue(List<String> list, int index) {

        if (list == null || index >= list.size()) {
            return null;
        }

        return list.get(index);
    }

    private Integer getCorrectOption(List<Integer> list, int index) {

        if (list == null || index >= list.size()) {
            return null;
        }

        return list.get(index);
    }

    @Transactional
    public List<PracticeQuestion> getPracticeQuestions(Long taskId) {

        Task task = repo.findById(taskId).orElse(null);

        if (task == null) {
            return new ArrayList<>();
        }

        return practiceQuestionRepository
                .findByTask(task)
                .stream()
                .sorted(Comparator.comparing(PracticeQuestion::getQuestionOrder))
                .collect(Collectors.toList());
    }

    @Transactional
    public Task addPracticeFromJson(Task task, Long courseId, User user, List<Map<String, Object>> questions) {
        Course course = courseRepository.findById(courseId).orElse(null);
        User teacher = userRepository.findById(user.getId()).orElse(null);
        if (course == null || teacher == null) return null;

        List<Map<String, Object>> validQuestions = questions == null ? new ArrayList<>() :
            questions.stream().filter(q -> q.get("questionText") != null && !q.get("questionText").toString().trim().isEmpty()).collect(Collectors.toList());

        int total = validQuestions.size();
        if (total != 5 && total != 10 && total != 20)
            throw new RuntimeException("La práctica debe tener exactamente 5, 10 o 20 preguntas.");

        int pts = total == 5 ? 4 : total == 10 ? 2 : 1;

        task.setCourse(course);
        task.setUser(teacher);
        task.setTaskType("PRACTICA");
        task.setPriority("MEDIA");
        Task saved = repo.save(task);

        int order = 1;
        for (Map<String, Object> qData : validQuestions) {
            PracticeQuestion q = new PracticeQuestion();
            q.setTask(saved);
            q.setQuestionText(qData.get("questionText").toString().trim());
            q.setPoints(pts);
            q.setQuestionOrder(order++);
            boolean written = Boolean.parseBoolean(qData.getOrDefault("writtenQuestion", "false").toString());
            q.setWrittenQuestion(written);
            PracticeQuestion savedQ = practiceQuestionRepository.save(q);

            if (!written) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> opts = (List<Map<String, Object>>) qData.getOrDefault("options", new ArrayList<>());
                int optOrder = 1;
                for (Map<String, Object> optData : opts) {
                    if (optData.get("optionText") == null || optData.get("optionText").toString().trim().isEmpty()) continue;
                    PracticeOption opt = new PracticeOption();
                    opt.setQuestion(savedQ);
                    opt.setOptionText(optData.get("optionText").toString().trim());
                    opt.setOptionOrder(optOrder++);
                    opt.setCorrect(Boolean.parseBoolean(optData.getOrDefault("correct", "false").toString()));
                    practiceOptionRepository.save(opt);
                }
            }
        }
        return saved;
    }

    @Transactional
    public void replacePracticeQuestions(Long taskId, List<Map<String, Object>> questions) {
        Task task = repo.findById(taskId).orElse(null);
        if (task == null) return;

        List<Map<String, Object>> valid = questions.stream()
            .filter(q -> q.get("questionText") != null && !q.get("questionText").toString().trim().isEmpty())
            .collect(Collectors.toList());

        int total = valid.size();
        if (total != 5 && total != 10 && total != 20)
            throw new RuntimeException("La práctica debe tener exactamente 5, 10 o 20 preguntas. Tienes " + total + ".");

        int pts = total == 5 ? 4 : total == 10 ? 2 : 1;

        practiceQuestionRepository.deleteByTask(task);
        practiceQuestionRepository.flush();

        int order = 1;
        for (Map<String, Object> qData : valid) {
            PracticeQuestion q = new PracticeQuestion();
            q.setTask(task);
            q.setQuestionText(qData.get("questionText").toString().trim());
            q.setPoints(pts);
            q.setQuestionOrder(order++);
            boolean written = Boolean.parseBoolean(qData.getOrDefault("writtenQuestion", "false").toString());
            q.setWrittenQuestion(written);
            PracticeQuestion savedQ = practiceQuestionRepository.save(q);

            if (!written) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> opts = (List<Map<String, Object>>) qData.getOrDefault("options", new ArrayList<>());
                int optOrder = 1;
                for (Map<String, Object> optData : opts) {
                    if (optData.get("optionText") == null || optData.get("optionText").toString().trim().isEmpty()) continue;
                    PracticeOption opt = new PracticeOption();
                    opt.setQuestion(savedQ);
                    opt.setOptionText(optData.get("optionText").toString().trim());
                    opt.setOptionOrder(optOrder++);
                    opt.setCorrect(Boolean.parseBoolean(optData.getOrDefault("correct", "false").toString()));
                    practiceOptionRepository.save(opt);
                }
            }
        }
    }

    @Transactional
    public void updateTask(Long taskId, User sessionUser, String description,
                           LocalDateTime dueDate, int maxAttempts, int maxFiles) {
        User teacher = userRepository.findById(sessionUser.getId()).orElse(null);
        Task task = repo.findById(taskId).orElse(null);
        if (teacher == null || task == null) return;
        if (!"DOCENTE".equals(teacher.getRole())) return;
        if (task.getUser() != null && !task.getUser().getId().equals(teacher.getId())) return;
        if (description != null) task.setDescription(description);
        if (dueDate != null) task.setDueDate(dueDate);
        if (maxAttempts >= 1) task.setMaxAttempts(Math.min(maxAttempts, 2));
        if (maxFiles >= 1) task.setMaxFiles(maxFiles);
        repo.save(task);
    }

    @Transactional
    public void deleteTask(Long taskId, User sessionUser) {

        User teacher =
                userRepository.findById(sessionUser.getId())
                        .orElse(null);

        Task task =
                repo.findById(taskId)
                        .orElse(null);

        if (teacher == null || task == null) {
            return;
        }

        if (!"DOCENTE".equals(teacher.getRole())) {
            return;
        }

        if (task.getUser() != null &&
            task.getUser().getId().equals(teacher.getId())) {

            studySessionRepository.deleteByTask(task);

            repo.delete(task);
        }
    }

    @Transactional
    public List<Task> getTasksByUser(User sessionUser) {

        User user =
                userRepository.findById(sessionUser.getId())
                        .orElse(null);

        if (user == null) {
            return new ArrayList<>();
        }

        List<Task> tasks = new ArrayList<>();

        if ("DOCENTE".equals(user.getRole())) {

            List<Course> courses =
                    courseRepository.findByUserId(user.getId());

            for (Course course : courses) {
                tasks.addAll(course.getTasks());
            }

        } else {

            for (Course course : user.getEnrolledCourses()) {
                tasks.addAll(course.getTasks());
            }
        }

        return tasks;
    }

    public List<Task> getTasksByCourse(Long courseId) {

        Course course =
                courseRepository.findById(courseId)
                        .orElse(null);

        if (course == null) {
            return List.of();
        }

        return repo.findByCourse(course);
    }

    public long totalTasks(User user) {
        return getTasksByUser(user).size();
    }

    public long completedTasks(User user) {

        if ("DOCENTE".equals(user.getRole())) {
            return receivedTasks(user);
        }

        User student =
                userRepository.findById(user.getId()).orElse(null);

        if (student == null) {
            return 0;
        }

        return submissionRepository.findByStudent(student).size();
    }

    public long pendingTasks(User user) {

        List<Task> tasks = getTasksByUser(user);

        if ("DOCENTE".equals(user.getRole())) {
            return notReceivedTasks(user);
        }

        User student =
                userRepository.findById(user.getId()).orElse(null);

        if (student == null) {
            return 0;
        }

        Set<Long> submittedTaskIds =
                submissionRepository.findByStudent(student)
                        .stream()
                        .map(s -> s.getTask().getId())
                        .collect(Collectors.toSet());

        return tasks
                .stream()
                .filter(t ->
                        !submittedTaskIds.contains(t.getId())
                                && (t.getDueDate() == null
                                || !t.getDueDate().isBefore(LocalDateTime.now())))
                .count();
    }

    public long overdueTasks(User user) {

        List<Task> tasks = getTasksByUser(user);

        if ("DOCENTE".equals(user.getRole())) {
            return notReceivedTasks(user);
        }

        User student =
                userRepository.findById(user.getId()).orElse(null);

        if (student == null) {
            return 0;
        }

        Set<Long> submittedTaskIds =
                submissionRepository.findByStudent(student)
                        .stream()
                        .map(s -> s.getTask().getId())
                        .collect(Collectors.toSet());

        return tasks
                .stream()
                .filter(t ->
                        !submittedTaskIds.contains(t.getId())
                                && t.getDueDate() != null
                                && t.getDueDate().isBefore(LocalDateTime.now()))
                .count();
    }

    @Transactional
    public long receivedTasks(User user) {

        if (!"DOCENTE".equals(user.getRole())) {
            return completedTasks(user);
        }

        List<Task> tasks = getTasksByUser(user);

        long totalReceived = 0;

        for (Task task : tasks) {
            totalReceived += submissionRepository.findByTask(task).size();
        }

        return totalReceived;
    }

    @Transactional
    public long notReceivedTasks(User user) {

        if (!"DOCENTE".equals(user.getRole())) {
            return overdueTasks(user);
        }

        List<Task> tasks = getTasksByUser(user);

        long totalNotReceived = 0;

        for (Task task : tasks) {

            Course course = task.getCourse();

            if (course == null || course.getStudents() == null) {
                continue;
            }

            int totalStudents = course.getStudents().size();

            List<Submission> submissions =
                    submissionRepository.findByTask(task);

            Set<Long> submittedStudentIds = new HashSet<>();

            for (Submission submission : submissions) {
                if (submission.getStudent() != null) {
                    submittedStudentIds.add(submission.getStudent().getId());
                }
            }

            totalNotReceived += Math.max(0, totalStudents - submittedStudentIds.size());
        }

        return totalNotReceived;
    }
}