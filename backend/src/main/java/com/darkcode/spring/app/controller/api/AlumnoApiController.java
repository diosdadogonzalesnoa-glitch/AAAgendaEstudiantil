package com.darkcode.spring.app.controller.api;

import com.darkcode.spring.app.model.*;
import com.darkcode.spring.app.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/alumno")
public class AlumnoApiController {

    @Autowired private UserService userService;
    @Autowired private CourseService courseService;
    @Autowired private TaskService taskService;
    @Autowired private SubmissionService submissionService;
    @Autowired private StudyService studyService;
    @Autowired private LessonService lessonService;

    private User u(Authentication auth) {
        return userService.findByEmail(auth.getName());
    }

    // ── Dashboard ─────────────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    public ResponseEntity<?> dashboard(Authentication auth) {
        User user = u(auth);
        if (user == null) return ResponseEntity.status(401).build();

        List<Course> courses = courseService.getEnrolledCoursesByStudent(user);
        List<Task> tasks = taskService.getTasksByUser(user);
        Set<Long> submitted = submissionService.getSubmittedTaskIds(user);
        Map<Long, Integer> minutesByCourse = studyService.getTotalMinutesByCourse(user);

        List<Map<String, Object>> chart = new ArrayList<>();
        double maxHours = courses.stream()
            .mapToDouble(c -> studyService.roundHours(minutesByCourse.getOrDefault(c.getId(), 0)))
            .max().orElse(1.0);
        if (maxHours == 0) maxHours = 1.0;
        for (Course c : courses) {
            double hours = studyService.roundHours(minutesByCourse.getOrDefault(c.getId(), 0));
            Map<String, Object> d = new HashMap<>();
            d.put("courseId", c.getId());
            d.put("courseName", c.getName());
            d.put("hours", hours);
            d.put("acronym", courseAcronym(c.getName()));
            d.put("heightPercent", (int) Math.round((hours / maxHours) * 80));
            chart.add(d);
        }

        List<Map<String, Object>> upcoming = new ArrayList<>();
        for (int i = 0; i < Math.min(6, tasks.size()); i++) {
            Task t = tasks.get(i);
            Map<String, Object> td = new HashMap<>();
            td.put("id", t.getId());
            td.put("title", t.getTitle());
            td.put("dueDate", t.getDueDate());
            td.put("priority", t.getPriority());
            td.put("taskType", t.getTaskType());
            td.put("submitted", submitted.contains(t.getId()));
            if (t.getCourse() != null)
                td.put("course", Map.of("id", t.getCourse().getId(), "name", t.getCourse().getName()));
            upcoming.add(td);
        }

        Map<String, Object> res = new HashMap<>();
        res.put("nombre", user.getName());
        res.put("totalCourses", courses.size());
        res.put("totalTasks", taskService.totalTasks(user));
        res.put("completedTasks", taskService.completedTasks(user));
        res.put("pendingTasks", taskService.pendingTasks(user));
        res.put("overdueTasks", taskService.overdueTasks(user));
        res.put("weeklyStudyHours", studyService.getWeeklyHoursByStudent(user));
        res.put("todayReminders", 0);
        res.put("submittedTaskIds", new ArrayList<>(submitted));
        res.put("upcomingTasks", upcoming);
        res.put("courseStudyChart", chart);
        return ResponseEntity.ok(res);
    }

    // ── Cursos ────────────────────────────────────────────────────────────────

    @GetMapping("/cursos")
    public ResponseEntity<?> myCourses(Authentication auth) {
        User user = u(auth);
        if (user == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(
            courseService.getEnrolledCoursesByStudent(user).stream().map(this::courseMap).toList()
        );
    }

    @GetMapping("/cursos/buscar")
    public ResponseEntity<?> searchCourses(@RequestParam String search, Authentication auth) {
        User user = u(auth);
        if (user == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(courseService.searchCourses(search).stream().map(this::courseMap).toList());
    }

    @PostMapping("/cursos/inscribir/{id}")
    public ResponseEntity<?> enroll(@PathVariable Long id, Authentication auth) {
        User user = u(auth);
        if (user == null) return ResponseEntity.status(401).build();
        courseService.enrollStudent(user, id);
        return ResponseEntity.ok(Map.of("message", "Inscrito correctamente"));
    }

    @GetMapping("/cursos/{id}")
    public ResponseEntity<?> courseDetail(@PathVariable Long id, Authentication auth) {
        User user = u(auth);
        if (user == null) return ResponseEntity.status(401).build();
        Course c = courseService.getCourseById(id);
        if (c == null) return ResponseEntity.notFound().build();

        List<Map<String, Object>> tasks = new ArrayList<>();
        for (Task t : c.getTasks()) {
            Submission sub = submissionService.getSubmissionByTaskAndStudent(t, user);
            Map<String, Object> td = new HashMap<>();
            td.put("id", t.getId());
            td.put("title", t.getTitle());
            td.put("description", t.getDescription());
            td.put("dueDate", t.getDueDate());
            td.put("priority", t.getPriority());
            td.put("taskType", t.getTaskType());
            td.put("status", sub == null ? "NO_ENTREGADO"
                    : sub.getGrade() == null ? "PENDIENTE_CALIFICACION" : "CALIFICADO");
            td.put("grade", sub != null ? sub.getGrade() : null);
            tasks.add(td);
        }

        Map<String, Object> res = new HashMap<>();
        res.put("course", courseMap(c));
        res.put("tasks", tasks);
        res.put("lessonGroups", lessonGroupsMap(lessonService.getLessonGroupsByCourse(id)));
        return ResponseEntity.ok(res);
    }

    // ── Tareas ────────────────────────────────────────────────────────────────

    @GetMapping("/tareas")
    public ResponseEntity<?> myTasks(Authentication auth) {
        User user = u(auth);
        if (user == null) return ResponseEntity.status(401).build();

        Map<Long, Submission> submissionByTask = submissionService.getSubmissionsByStudentMappedByTask(user);

        List<Map<String, Object>> taskList = taskService.getTasksByUser(user).stream().map(t -> {
            Submission sub = submissionByTask.get(t.getId());
            Map<String, Object> td = new HashMap<>();
            td.put("id", t.getId());
            td.put("title", t.getTitle());
            td.put("description", t.getDescription());
            td.put("dueDate", t.getDueDate());
            td.put("priority", t.getPriority());
            td.put("taskType", t.getTaskType());
            td.put("submitted", sub != null);
            td.put("grade", sub != null ? sub.getGrade() : null);
            td.put("feedback", sub != null ? sub.getFeedback() : null);
            td.put("maxAttempts", t.getMaxAttempts());
            td.put("maxFiles", t.getMaxFiles());
            td.put("attemptNumber", sub != null ? sub.getAttemptNumber() : 0);
            td.put("submissionStatus", sub == null ? "NO_ENTREGADO"
                    : sub.getGrade() == null ? "PENDIENTE_CALIFICACION" : "CALIFICADO");
            if (t.getCourse() != null)
                td.put("course", Map.of("id", t.getCourse().getId(), "name", t.getCourse().getName()));
            return td;
        }).toList();

        Map<String, Object> res = new HashMap<>();
        res.put("totalTasks", taskService.totalTasks(user));
        res.put("completedTasks", taskService.completedTasks(user));
        res.put("pendingTasks", taskService.pendingTasks(user));
        res.put("overdueTasks", taskService.overdueTasks(user));
        res.put("tasks", taskList);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/tareas/{id}")
    public ResponseEntity<?> taskDetail(@PathVariable Long id, Authentication auth) {
        User user = u(auth);
        if (user == null) return ResponseEntity.status(401).build();
        Task t = taskService.getTaskById(id);
        if (t == null) return ResponseEntity.notFound().build();
        Submission sub = submissionService.getSubmissionByTaskAndStudent(t, user);

        Map<String, Object> res = new HashMap<>();
        res.put("id", t.getId());
        res.put("title", t.getTitle());
        res.put("description", t.getDescription());
        res.put("dueDate", t.getDueDate());
        res.put("priority", t.getPriority());
        res.put("taskType", t.getTaskType());
        res.put("submitted", sub != null);
        res.put("grade", sub != null ? sub.getGrade() : null);
        if (t.getCourse() != null)
            res.put("course", Map.of("id", t.getCourse().getId(), "name", t.getCourse().getName()));
        return ResponseEntity.ok(res);
    }

    @PostMapping("/tareas/{id}/entregar")
    public ResponseEntity<?> submitTask(@PathVariable Long id,
                                        @RequestParam(required = false) String answerText,
                                        @RequestParam(required = false) MultipartFile[] files,
                                        Authentication auth) {
        User user = u(auth);
        if (user == null) return ResponseEntity.status(401).build();
        try {
            submissionService.submitTask(id, user, answerText, files);
            return ResponseEntity.ok(Map.of("message", "Entrega realizada"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/tareas/{id}/practica")
    public ResponseEntity<?> getPractica(@PathVariable Long id, Authentication auth) {
        User user = u(auth);
        if (user == null) return ResponseEntity.status(401).build();
        Task task = taskService.getTaskById(id);
        if (task == null) return ResponseEntity.notFound().build();
        Submission sub = submissionService.getSubmissionByTaskAndStudent(task, user);

        List<PracticeQuestion> questions = taskService.getPracticeQuestions(id);

        // Build answer lookup if already submitted
        Map<Long, PracticeAnswer> answerByQuestion = new HashMap<>();
        if (sub != null) {
            for (PracticeAnswer pa : submissionService.getPracticeAnswersBySubmission(sub))
                answerByQuestion.put(pa.getQuestion().getId(), pa);
        }

        List<Map<String, Object>> qList = questions.stream().map(q -> {
            Map<String, Object> qd = new HashMap<>();
            qd.put("id", q.getId());
            qd.put("text", q.getQuestionText());
            qd.put("written", q.isWrittenQuestion());
            qd.put("order", q.getQuestionOrder());

            List<Map<String, Object>> opts = new ArrayList<>();
            if (q.getOptions() != null) {
                for (PracticeOption o : q.getOptions()) {
                    Map<String, Object> om = new HashMap<>();
                    om.put("id", o.getId());
                    om.put("text", o.getOptionText());
                    om.put("order", o.getOptionOrder());
                    om.put("correct", o.isCorrect());
                    opts.add(om);
                }
                opts.sort(Comparator.comparing(o -> (Integer) o.get("order")));
            }
            qd.put("options", opts);

            // Include student's answer for review
            PracticeAnswer pa = answerByQuestion.get(q.getId());
            if (pa != null) {
                qd.put("isCorrect", pa.getCorrect());
                qd.put("writtenAnswer", pa.getWrittenAnswer());
                if (pa.getSelectedOption() != null)
                    qd.put("selectedOptionId", pa.getSelectedOption().getId());
            }

            return qd;
        }).sorted(Comparator.comparing(q -> (Integer) q.get("order"))).toList();

        Map<String, Object> taskMap = new HashMap<>();
        taskMap.put("id", task.getId());
        taskMap.put("title", task.getTitle());
        taskMap.put("description", task.getDescription());
        taskMap.put("dueDate", task.getDueDate());
        if (task.getCourse() != null)
            taskMap.put("course", Map.of("id", task.getCourse().getId(), "name", task.getCourse().getName()));

        Map<String, Object> response = new HashMap<>();
        response.put("tarea", taskMap);
        response.put("questions", qList);
        response.put("submitted", sub != null);
        if (sub != null) {
            Map<String, Object> subMap = new HashMap<>();
            subMap.put("id", sub.getId());
            subMap.put("grade", sub.getGrade());
            subMap.put("feedback", sub.getFeedback());
            response.put("submission", subMap);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/tareas/{id}/practica/submit")
    public ResponseEntity<?> submitPractica(@PathVariable Long id,
                                             @RequestBody Map<String, Object> body,
                                             Authentication auth) {
        User user = u(auth);
        if (user == null) return ResponseEntity.status(401).build();
        @SuppressWarnings("unchecked")
        Map<String, String> answers = (Map<String, String>) body.getOrDefault("answers", new HashMap<>());
        submissionService.submitPracticeTask(id, user, answers);
        try {
            if (body.get("courseId") != null && body.get("startedAt") != null && body.get("finishedAt") != null) {
                Long courseId = Long.parseLong(body.get("courseId").toString());
                LocalDateTime startedAt = LocalDateTime.parse(body.get("startedAt").toString());
                LocalDateTime finishedAt = LocalDateTime.parse(body.get("finishedAt").toString());
                studyService.saveStudySession(user, courseId, id, startedAt, finishedAt);
            }
        } catch (Exception ignored) {}
        return ResponseEntity.ok(Map.of("message", "Práctica enviada"));
    }

    // ── Estudio ───────────────────────────────────────────────────────────────

    @GetMapping("/estudio")
    public ResponseEntity<?> estudio(Authentication auth) {
        User user = u(auth);
        if (user == null) return ResponseEntity.status(401).build();
        List<Course> courses = courseService.getEnrolledCoursesByStudent(user);
        Set<Long> submitted = submissionService.getSubmittedTaskIds(user);

        List<Map<String, Object>> pending = taskService.getTasksByUser(user).stream()
            .filter(t -> "PRACTICA".equals(t.getTaskType()) && !submitted.contains(t.getId()))
            .map(t -> {
                Map<String, Object> td = new HashMap<>();
                td.put("id", t.getId());
                td.put("title", t.getTitle());
                if (t.getCourse() != null)
                    td.put("course", Map.of("id", t.getCourse().getId(), "name", t.getCourse().getName()));
                return td;
            }).toList();

        Map<String, Object> res = new HashMap<>();
        res.put("courses", courses.stream().map(this::courseMap).toList());
        res.put("pendingPracticeTasks", pending);
        res.put("weeklyStudyHours", studyService.getWeeklyHoursByStudent(user));
        res.put("totalSessions", studyService.getTotalSessionsByStudent(user));
        res.put("courseStats", studyService.getCourseStatsForStudent(user, courses));
        return ResponseEntity.ok(res);
    }

    @PostMapping("/estudio/guardar")
    public ResponseEntity<?> saveStudySession(@RequestBody Map<String, Object> body, Authentication auth) {
        User user = u(auth);
        if (user == null) return ResponseEntity.status(401).build();
        try {
            Long courseId = Long.parseLong(body.get("courseId").toString());
            Long taskId = Long.parseLong(body.get("taskId").toString());
            LocalDateTime startedAt = LocalDateTime.parse(body.get("startedAt").toString());
            LocalDateTime finishedAt = LocalDateTime.parse(body.get("finishedAt").toString());
            studyService.saveStudySession(user, courseId, taskId, startedAt, finishedAt);
            return ResponseEntity.ok(Map.of("message", "Sesión guardada"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Datos inválidos"));
        }
    }

    // ── Lecciones ─────────────────────────────────────────────────────────────

    @GetMapping("/lecciones/{id}")
    public ResponseEntity<?> lessonDetail(@PathVariable Long id, Authentication auth) {
        User user = u(auth);
        if (user == null) return ResponseEntity.status(401).build();
        Lesson lesson = lessonService.getLessonById(id);
        if (lesson == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(Map.of("lesson", lessonMap(lesson)));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Map<String, Object> courseMap(Course c) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", c.getId());
        m.put("name", c.getName());
        m.put("teacher", c.getTeacher());
        m.put("schedule", c.getSchedule());
        m.put("status", c.getStatus());
        m.put("studentsCount", c.getStudents() != null ? c.getStudents().size() : 0);
        m.put("tasksCount", c.getTasks() != null ? c.getTasks().size() : 0);
        return m;
    }

    private Map<String, Object> lessonMap(Lesson l) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", l.getId());
        m.put("title", l.getTitle());
        m.put("description", l.getDescription());
        m.put("storedFileName", l.getStoredFileName());
        m.put("originalFileName", l.getOriginalFileName());
        m.put("lessonDate", l.getLessonDate());
        if (l.getCourse() != null)
            m.put("course", Map.of("id", l.getCourse().getId(), "name", l.getCourse().getName()));
        return m;
    }

    private String courseAcronym(String name) {
        if (name == null) return "";
        StringBuilder sb = new StringBuilder();
        for (String word : name.trim().split("\\s+")) {
            if (!word.isEmpty() && Character.isUpperCase(word.charAt(0)))
                sb.append(word.charAt(0));
        }
        return sb.length() > 0 ? sb.toString() : name.substring(0, Math.min(3, name.length())).toUpperCase();
    }

    private List<Map<String, Object>> lessonGroupsMap(List<LessonGroup> groups) {
        return groups.stream().map(g -> {
            Map<String, Object> gm = new HashMap<>();
            gm.put("title", g.getTitle());
            gm.put("lessons", g.getLessons().stream().map(this::lessonMap).toList());
            return gm;
        }).toList();
    }
}
