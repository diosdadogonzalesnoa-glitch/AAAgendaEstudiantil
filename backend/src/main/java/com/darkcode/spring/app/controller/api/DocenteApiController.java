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
@RequestMapping("/api/docente")
public class DocenteApiController {

    @Autowired private UserService userService;
    @Autowired private CourseService courseService;
    @Autowired private TaskService taskService;
    @Autowired private SubmissionService submissionService;
    @Autowired private LessonService lessonService;
    @Autowired private com.darkcode.spring.app.service.StudyService studyService;
    @Autowired private com.darkcode.spring.app.repository.PracticeQuestionRepository practiceQuestionRepository;
    @Autowired private com.darkcode.spring.app.repository.PracticeOptionRepository practiceOptionRepository;

    private User u(Authentication auth) {
        return userService.findByEmail(auth.getName());
    }

    // ── Dashboard ─────────────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    public ResponseEntity<?> dashboard(Authentication auth) {
        User user = u(auth);
        if (user == null) return ResponseEntity.status(401).build();

        List<Course> courses = courseService.getCoursesByUser(user);

        List<Task> allTasks = new ArrayList<>();
        for (Course c : courses) allTasks.addAll(c.getTasks());

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        long overdue = allTasks.stream()
            .filter(t -> t.getDueDate() != null && t.getDueDate().isBefore(now))
            .count();

        List<Map<String, Object>> upcoming = allTasks.stream()
            .sorted((a, b) -> {
                if (a.getDueDate() == null) return 1;
                if (b.getDueDate() == null) return -1;
                return a.getDueDate().compareTo(b.getDueDate());
            })
            .limit(6)
            .map(t -> {
                Map<String, Object> td = new HashMap<>();
                td.put("id", t.getId());
                td.put("title", t.getTitle());
                td.put("dueDate", t.getDueDate());
                td.put("priority", t.getPriority());
                td.put("taskType", t.getTaskType());
                td.put("submitted", false);
                if (t.getCourse() != null)
                    td.put("course", Map.of("id", t.getCourse().getId(), "name", t.getCourse().getName()));
                return td;
            }).collect(java.util.stream.Collectors.toList());

        List<Map<String, Object>> chart = new ArrayList<>();
        for (Course c : courses) {
            Map<String, Object> d = new HashMap<>();
            d.put("courseId", c.getId());
            d.put("courseName", c.getName());
            d.put("hours", 0.0);
            d.put("acronym", courseAcronym(c.getName()));
            d.put("heightPercent", 0);
            chart.add(d);
        }

        Map<String, Object> res = new HashMap<>();
        res.put("nombre", user.getName());
        res.put("totalCourses", courses.size());
        res.put("totalTasks", allTasks.size());
        res.put("overdueTasks", overdue);
        res.put("weeklyStudyHours", 0.0);
        res.put("todayReminders", 0);
        res.put("submittedTaskIds", new ArrayList<>());
        res.put("upcomingTasks", upcoming);
        res.put("courseStudyChart", chart);
        return ResponseEntity.ok(res);
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

    // ── Estudio ───────────────────────────────────────────────────────────────

    @GetMapping("/estudio")
    public ResponseEntity<?> estudio(Authentication auth) {
        User user = u(auth);
        if (user == null) return ResponseEntity.status(401).build();

        List<Course> courses = courseService.getCoursesByUser(user);
        Map<Long, List<Map<String, Object>>> statsByCourse = studyService.getTeacherCourseStudentStats(courses);

        List<Map<String, Object>> courseList = courses.stream()
            .map(c -> Map.<String, Object>of("id", c.getId(), "name", c.getName()))
            .toList();

        Map<String, List<Map<String, Object>>> statsStringKeys = new HashMap<>();
        for (Map.Entry<Long, List<Map<String, Object>>> entry : statsByCourse.entrySet()) {
            statsStringKeys.put(entry.getKey().toString(), entry.getValue());
        }

        return ResponseEntity.ok(Map.of("courses", courseList, "statsByCourse", statsStringKeys));
    }

    // ── Cursos ────────────────────────────────────────────────────────────────

    @GetMapping("/cursos")
    public ResponseEntity<?> myCourses(Authentication auth) {
        User user = u(auth);
        if (user == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(courseService.getCoursesByUser(user).stream().map(this::courseMap).toList());
    }

    @PostMapping("/cursos")
    public ResponseEntity<?> addCourse(@RequestBody Map<String, Object> body, Authentication auth) {
        User user = u(auth);
        if (user == null) return ResponseEntity.status(401).build();
        Course c = new Course();
        c.setName(str(body, "name"));
        c.setTeacher(str(body, "teacher"));
        c.setSchedule(str(body, "schedule"));
        courseService.addCourse(c, user);
        return ResponseEntity.ok(Map.of("message", "Curso creado"));
    }

    @PutMapping("/cursos/{id}")
    public ResponseEntity<?> updateCourse(@PathVariable Long id,
                                           @RequestBody Map<String, Object> body,
                                           Authentication auth) {
        User user = u(auth);
        if (user == null) return ResponseEntity.status(401).build();
        courseService.updateCourse(id, str(body, "name"), str(body, "teacher"), str(body, "schedule"));
        return ResponseEntity.ok(Map.of("message", "Curso actualizado"));
    }

    @PatchMapping("/cursos/{id}/status")
    public ResponseEntity<?> updateCourseStatus(@PathVariable Long id,
                                                 @RequestBody Map<String, Object> body,
                                                 Authentication auth) {
        User user = u(auth);
        if (user == null) return ResponseEntity.status(401).build();
        String status = body.getOrDefault("status", "ACTIVO").toString();
        courseService.updateCourseStatus(id, status);
        return ResponseEntity.ok(Map.of("message", "Estado actualizado"));
    }

    @DeleteMapping("/cursos/{id}")
    public ResponseEntity<?> deleteCourse(@PathVariable Long id, Authentication auth) {
        User user = u(auth);
        if (user == null) return ResponseEntity.status(401).build();
        courseService.deleteCourse(id, user);
        return ResponseEntity.ok(Map.of("message", "Curso eliminado"));
    }

    @GetMapping("/cursos/{id}")
    public ResponseEntity<?> courseDetail(@PathVariable Long id, Authentication auth) {
        User user = u(auth);
        if (user == null) return ResponseEntity.status(401).build();
        Course c = courseService.getCourseById(id);
        if (c == null) return ResponseEntity.notFound().build();

        List<Map<String, Object>> tasks = c.getTasks().stream().map(t -> {
            List<Submission> subs = submissionService.getSubmissionsByTask(t.getId());
            Map<String, Object> td = new HashMap<>();
            td.put("id", t.getId());
            td.put("title", t.getTitle());
            td.put("description", t.getDescription());
            td.put("dueDate", t.getDueDate());
            td.put("priority", t.getPriority());
            td.put("taskType", t.getTaskType());
            td.put("maxAttempts", t.getMaxAttempts());
            td.put("maxFiles", t.getMaxFiles());
            td.put("submissionsCount", subs.size());
            return td;
        }).toList();

        Map<String, Object> res = new HashMap<>();
        res.put("course", courseMap(c));
        res.put("tasks", tasks);
        res.put("lessonGroups", lessonGroupsMap(lessonService.getLessonGroupsByCourse(id)));
        return ResponseEntity.ok(res);
    }

    // ── Tareas dentro de un curso ─────────────────────────────────────────────

    @PostMapping("/cursos/{id}/tareas")
    public ResponseEntity<?> addTarea(@PathVariable Long id,
                                       @RequestBody Map<String, Object> body,
                                       Authentication auth) {
        User user = u(auth);
        if (user == null) return ResponseEntity.status(401).build();
        Task t = new Task();
        t.setTitle(str(body, "title"));
        t.setDescription(str(body, "description"));
        t.setPriority(str(body, "priority") != null ? str(body, "priority") : "MEDIA");
        t.setTaskType(str(body, "taskType") != null ? str(body, "taskType") : "TAREA");
        String due = str(body, "dueDate");
        if (due != null && !due.isEmpty()) {
            try { t.setDueDate(LocalDateTime.parse(due)); } catch (Exception ignored) {}
        }
        try { t.setMaxAttempts(Math.min(2, Math.max(1, Integer.parseInt(body.getOrDefault("maxAttempts", 1).toString())))); } catch (Exception ignored) {}
        try { t.setMaxFiles(Math.max(1, Integer.parseInt(body.getOrDefault("maxFiles", 20).toString()))); } catch (Exception ignored) {}
        taskService.addTask(t, id, user);
        return ResponseEntity.ok(Map.of("message", "Tarea publicada"));
    }

    @PutMapping("/tareas/{id}")
    public ResponseEntity<?> editTarea(@PathVariable Long id,
                                        @RequestBody Map<String, Object> body,
                                        Authentication auth) {
        User user = u(auth);
        if (user == null) return ResponseEntity.status(401).build();
        String description = str(body, "description");
        String due = str(body, "dueDate");
        LocalDateTime dueDate = null;
        if (due != null && !due.isEmpty()) {
            try { dueDate = LocalDateTime.parse(due); } catch (Exception ignored) {}
        }
        int maxAttempts = 1;
        int maxFiles = 20;
        try { maxAttempts = Integer.parseInt(body.getOrDefault("maxAttempts", 1).toString()); } catch (Exception ignored) {}
        try { maxFiles = Integer.parseInt(body.getOrDefault("maxFiles", 20).toString()); } catch (Exception ignored) {}
        taskService.updateTask(id, user, description, dueDate, maxAttempts, maxFiles);
        return ResponseEntity.ok(Map.of("message", "Tarea actualizada"));
    }

    @PostMapping("/cursos/{id}/practicas")
    public ResponseEntity<?> addPractica(@PathVariable Long id,
                                          @RequestBody Map<String, Object> body,
                                          Authentication auth) {
        User user = u(auth);
        if (user == null) return ResponseEntity.status(401).build();
        Task t = new Task();
        t.setTitle(str(body, "title"));
        t.setDescription(str(body, "description"));
        String due = str(body, "dueDate");
        if (due != null && !due.isEmpty()) {
            try { t.setDueDate(LocalDateTime.parse(due)); } catch (Exception ignored) {}
        }
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> questions = (List<Map<String, Object>>) body.getOrDefault("questions", new java.util.ArrayList<>());
            taskService.addPracticeFromJson(t, id, user, questions);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
        return ResponseEntity.ok(Map.of("message", "Práctica publicada"));
    }

    @PostMapping("/cursos/{id}/lecciones")
    public ResponseEntity<?> addLesson(@PathVariable Long id,
                                        @RequestParam String title,
                                        @RequestParam(required = false) String description,
                                        @RequestParam(required = false) String groupTitle,
                                        @RequestParam("file") MultipartFile file,
                                        Authentication auth) {
        User user = u(auth);
        if (user == null) return ResponseEntity.status(401).build();
        lessonService.addLesson(id, user, title, description, file);
        return ResponseEntity.ok(Map.of("message", "Lección publicada"));
    }

    // ── Tareas ────────────────────────────────────────────────────────────────

    @GetMapping("/tareas")
    public ResponseEntity<?> myTasks(Authentication auth) {
        User user = u(auth);
        if (user == null) return ResponseEntity.status(401).build();

        List<Map<String, Object>> taskList = taskService.getTasksByUser(user).stream().map(t -> {
            Map<String, Object> td = new HashMap<>();
            td.put("id", t.getId());
            td.put("title", t.getTitle());
            td.put("description", t.getDescription());
            td.put("dueDate", t.getDueDate());
            td.put("priority", t.getPriority());
            td.put("taskType", t.getTaskType());
            if (t.getCourse() != null)
                td.put("course", Map.of("id", t.getCourse().getId(), "name", t.getCourse().getName()));
            return td;
        }).toList();

        Map<String, Object> res = new HashMap<>();
        res.put("tasks", taskList);
        res.put("receivedTasks", taskService.receivedTasks(user));
        res.put("notReceivedTasks", taskService.notReceivedTasks(user));
        return ResponseEntity.ok(res);
    }

    @DeleteMapping("/tareas/{id}")
    public ResponseEntity<?> deleteTask(@PathVariable Long id, Authentication auth) {
        User user = u(auth);
        if (user == null) return ResponseEntity.status(401).build();
        taskService.deleteTask(id, user);
        return ResponseEntity.ok(Map.of("message", "Tarea eliminada"));
    }

    // ── Entregas ──────────────────────────────────────────────────────────────

    @GetMapping("/tareas/{id}/entregas")
    public ResponseEntity<?> submissions(@PathVariable Long id, Authentication auth) {
        User user = u(auth);
        if (user == null) return ResponseEntity.status(401).build();
        Task task = taskService.getTaskById(id);
        if (task == null) return ResponseEntity.notFound().build();

        List<Submission> subs = submissionService.getSubmissionsByTask(id);
        Map<Long, List<Map<String, String>>> sfMap = new HashMap<>();
        List<Map<String, Object>> subList = subs.stream().map(s -> {
            Map<String, Object> sd = new HashMap<>();
            sd.put("id", s.getId());
            sd.put("grade", s.getGrade());
            sd.put("answerText", s.getAnswerText());
            sd.put("submittedAt", s.getSubmittedAt());
            sd.put("feedback", s.getFeedback());
            sd.put("attemptNumber", s.getAttemptNumber());
            if (s.getStudent() != null)
                sd.put("student", Map.of("id", s.getStudent().getId(), "name", s.getStudent().getName(), "email", s.getStudent().getEmail()));
            List<Map<String, String>> files = submissionService.getFilesFromSubmission(s);
            sfMap.put(s.getId(), files);
            return sd;
        }).toList();

        Map<String, Object> taskMap = new HashMap<>();
        taskMap.put("id", task.getId());
        taskMap.put("title", task.getTitle());
        taskMap.put("taskType", task.getTaskType());
        taskMap.put("maxAttempts", task.getMaxAttempts());
        taskMap.put("maxFiles", task.getMaxFiles());

        List<User> pending = submissionService.getPendingStudentsByTask(id);
        List<Map<String, Object>> pendingList = pending.stream()
            .map(s -> Map.<String, Object>of("id", s.getId(), "name", s.getName(), "email", s.getEmail()))
            .toList();

        Map<String, Object> res = new HashMap<>();
        res.put("task", taskMap);
        res.put("submissions", subList);
        res.put("pendingStudents", pendingList);
        res.put("practiceAnswersMap", submissionService.getSerializedPracticeAnswersByTask(id));
        res.put("submissionFiles", sfMap);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/entregas/{id}/calificar")
    public ResponseEntity<?> grade(@PathVariable Long id,
                                    @RequestBody Map<String, Object> body,
                                    Authentication auth) {
        User user = u(auth);
        if (user == null) return ResponseEntity.status(401).build();
        int grade = Integer.parseInt(body.get("grade").toString());
        submissionService.gradeSubmission(id, grade);
        return ResponseEntity.ok(Map.of("message", "Calificado correctamente"));
    }

    @PutMapping("/answers/{answerId}/correct")
    public ResponseEntity<?> markAnswerCorrect(@PathVariable Long answerId,
                                               @RequestBody Map<String, Object> body,
                                               Authentication auth) {
        User user = u(auth);
        if (user == null) return ResponseEntity.status(401).build();
        boolean correct = Boolean.parseBoolean(body.getOrDefault("correct", "false").toString());
        int newGrade = submissionService.markAnswerCorrect(answerId, correct);
        return ResponseEntity.ok(Map.of("grade", newGrade));
    }

    @PostMapping("/entregas/{id}/feedback")
    public ResponseEntity<?> feedback(@PathVariable Long id,
                                       @RequestBody Map<String, Object> body,
                                       Authentication auth) {
        User user = u(auth);
        if (user == null) return ResponseEntity.status(401).build();
        String feedback = body.getOrDefault("feedback", "").toString();
        submissionService.addFeedback(id, feedback);
        return ResponseEntity.ok(Map.of("message", "Retroalimentación guardada"));
    }

    // ── Prácticas (edición) ───────────────────────────────────────────────────

    @GetMapping("/practicas/{taskId}/preguntas")
    public ResponseEntity<?> getPracticeQuestions(@PathVariable Long taskId, Authentication auth) {
        User user = u(auth);
        if (user == null) return ResponseEntity.status(401).build();
        Task task = taskService.getTaskById(taskId);
        if (task == null) return ResponseEntity.notFound().build();

        List<com.darkcode.spring.app.model.PracticeQuestion> questions = taskService.getPracticeQuestions(taskId);
        List<Map<String, Object>> result = questions.stream().map(q -> {
            Map<String, Object> qm = new HashMap<>();
            qm.put("id", q.getId());
            qm.put("questionText", q.getQuestionText());
            qm.put("writtenQuestion", q.isWrittenQuestion());
            qm.put("questionOrder", q.getQuestionOrder());
            qm.put("points", q.getPoints());
            List<Map<String, Object>> opts = practiceOptionRepository
                .findByQuestionOrderByOptionOrderAsc(q).stream().map(o -> {
                    Map<String, Object> om = new HashMap<>();
                    om.put("id", o.getId());
                    om.put("optionText", o.getOptionText());
                    om.put("optionOrder", o.getOptionOrder());
                    om.put("correct", o.isCorrect());
                    return om;
                }).toList();
            qm.put("options", opts);
            return qm;
        }).toList();

        return ResponseEntity.ok(result);
    }

    @PutMapping("/practicas/{taskId}")
    public ResponseEntity<?> updatePractica(@PathVariable Long taskId,
                                             @RequestBody Map<String, Object> body,
                                             Authentication auth) {
        User user = u(auth);
        if (user == null) return ResponseEntity.status(401).build();
        Task task = taskService.getTaskById(taskId);
        if (task == null) return ResponseEntity.notFound().build();

        String title = str(body, "title");
        String description = str(body, "description");
        String due = str(body, "dueDate");
        if (title != null && !title.isBlank()) task.setTitle(title);
        if (description != null) task.setDescription(description);
        if (due != null && !due.isEmpty()) {
            try { task.setDueDate(java.time.LocalDateTime.parse(due)); } catch (Exception ignored) {}
        }
        taskService.save(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> questions = (List<Map<String, Object>>) body.get("questions");
        if (questions != null) {
            int subCount = submissionService.getSubmissionsByTask(taskId).size();
            if (subCount > 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "No puedes modificar las preguntas porque ya existen " + subCount + " entrega(s)."));
            }
            try {
                taskService.replacePracticeQuestions(taskId, questions);
            } catch (RuntimeException e) {
                return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
            }
        }
        return ResponseEntity.ok(Map.of("message", "Práctica actualizada"));
    }

    // ── Lecciones ─────────────────────────────────────────────────────────────

    @GetMapping("/lecciones")
    public ResponseEntity<?> allLessons(Authentication auth) {
        User user = u(auth);
        if (user == null) return ResponseEntity.status(401).build();
        List<Course> courses = courseService.getCoursesByUser(user);
        List<Map<String, Object>> lessons = new ArrayList<>();
        for (Course c : courses) {
            for (LessonGroup group : lessonService.getLessonGroupsByCourse(c.getId())) {
                for (Lesson l : group.getLessons()) {
                    Map<String, Object> lm = lessonMap(l);
                    lm.put("course", Map.of("id", c.getId(), "name", c.getName()));
                    lessons.add(lm);
                }
            }
        }
        lessons.sort(Comparator.comparing(l -> l.get("lessonDate") != null ? l.get("lessonDate").toString() : ""));
        return ResponseEntity.ok(lessons);
    }

    @GetMapping("/lecciones/{id}")
    public ResponseEntity<?> lessonDetail(@PathVariable Long id, Authentication auth) {
        User user = u(auth);
        if (user == null) return ResponseEntity.status(401).build();
        Lesson lesson = lessonService.getLessonById(id);
        if (lesson == null) return ResponseEntity.notFound().build();
        Map<String, Object> lm = lessonMap(lesson);
        if (lesson.getCourse() != null)
            lm.put("course", Map.of("id", lesson.getCourse().getId(), "name", lesson.getCourse().getName()));
        return ResponseEntity.ok(Map.of("lesson", lm));
    }

    @DeleteMapping("/lecciones/{id}")
    public ResponseEntity<?> deleteLesson(@PathVariable Long id, Authentication auth) {
        User user = u(auth);
        if (user == null) return ResponseEntity.status(401).build();
        lessonService.deleteLesson(id, user);
        return ResponseEntity.ok(Map.of("message", "Lección eliminada"));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String str(Map<String, Object> body, String key) {
        Object v = body.get(key);
        return v != null ? v.toString() : null;
    }

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
        return m;
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
