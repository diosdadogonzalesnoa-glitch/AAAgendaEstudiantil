package com.darkcode.spring.app.controller;

import com.darkcode.spring.app.model.PracticeOption;
import com.darkcode.spring.app.model.PracticeQuestion;
import com.darkcode.spring.app.model.Submission;
import com.darkcode.spring.app.model.Task;
import com.darkcode.spring.app.model.User;
import com.darkcode.spring.app.service.StudyService;
import com.darkcode.spring.app.service.SubmissionService;
import com.darkcode.spring.app.service.TaskService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;

@Controller
public class TaskController {

    @Autowired
    private TaskService taskService;

    @Autowired
    private SubmissionService submissionService;

    @Autowired
    private StudyService studyService;

    @GetMapping("/tasks")
    public String tasks(Model model,
                        HttpSession session) {

        User user = (User) session.getAttribute("user");

        if (user == null) {
            return "redirect:/login";
        }

        String primerNombre = user.getName().split(" ")[0];

        model.addAttribute("nombre", primerNombre);
        model.addAttribute("tasks", taskService.getTasksByUser(user));
        model.addAttribute("totalTasks", taskService.totalTasks(user));
        model.addAttribute("completedTasks", taskService.completedTasks(user));
        model.addAttribute("pendingTasks", taskService.pendingTasks(user));
        model.addAttribute("overdueTasks", taskService.overdueTasks(user));
        model.addAttribute("receivedTasks", taskService.receivedTasks(user));
        model.addAttribute("notReceivedTasks", taskService.notReceivedTasks(user));
        model.addAttribute("submittedTaskIds", submissionService.getSubmittedTaskIds(user));

        return "tasks";
    }

    @PostMapping("/tasks/add")
    public String addTask(@ModelAttribute Task task,
                          @RequestParam Long courseId,
                          HttpSession session) {

        User user = (User) session.getAttribute("user");

        if (user == null) {
            return "redirect:/login";
        }

        if (!"DOCENTE".equals(user.getRole())) {
            return "redirect:/courses/" + courseId;
        }

        taskService.addTask(task, courseId, user);

        return "redirect:/courses/" + courseId;
    }

    @PostMapping("/tasks/add-practice")
    public String addPracticeTask(@RequestParam Long courseId,
                                  @RequestParam String title,
                                  @RequestParam(required = false) String description,
                                  @RequestParam(required = false) String priority,
                                  @RequestParam(required = false)
                                  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                  LocalDateTime dueDate,
                                  @RequestParam(required = false, name = "questionText") List<String> questionTexts,
                                  @RequestParam(required = false, name = "option1") List<String> option1,
                                  @RequestParam(required = false, name = "option2") List<String> option2,
                                  @RequestParam(required = false, name = "option3") List<String> option3,
                                  @RequestParam(required = false, name = "option4") List<String> option4,
                                  @RequestParam(required = false, name = "option5") List<String> option5,
                                  @RequestParam(required = false, name = "correctOption") List<Integer> correctOptions,
                                  HttpSession session) {

        User user = (User) session.getAttribute("user");

        if (user == null) {
            return "redirect:/login";
        }

        if (!"DOCENTE".equals(user.getRole())) {
            return "redirect:/courses/" + courseId;
        }

        Task task = new Task();
        task.setTitle(title);
        task.setDescription(description);
        task.setPriority(priority);
        task.setDueDate(dueDate);

        taskService.addPracticeTask(
                task,
                courseId,
                user,
                questionTexts,
                option1,
                option2,
                option3,
                option4,
                option5,
                correctOptions
        );

        return "redirect:/courses/" + courseId;
    }

    @GetMapping("/api/practice/{id}")
    @ResponseBody
    public List<Map<String, Object>> getPracticeQuestionsApi(@PathVariable Long id,
                                                             HttpSession session) {

        User user = (User) session.getAttribute("user");

        if (user == null) {
            return new ArrayList<>();
        }

        Task task = taskService.getTaskById(id);

        if (task == null || !"PRACTICA".equals(task.getTaskType())) {
            return new ArrayList<>();
        }

        List<PracticeQuestion> questions =
                taskService.getPracticeQuestions(id);

        List<Map<String, Object>> response = new ArrayList<>();

        for (PracticeQuestion question : questions) {

            Map<String, Object> questionData = new HashMap<>();

            questionData.put("id", question.getId());
            questionData.put("text", question.getQuestionText());
            questionData.put("written", question.isWrittenQuestion());
            questionData.put("order", question.getQuestionOrder());

            List<Map<String, Object>> options = new ArrayList<>();

            if (question.getOptions() != null) {
                for (PracticeOption option : question.getOptions()) {

                    Map<String, Object> optionData = new HashMap<>();

                    optionData.put("id", option.getId());
                    optionData.put("text", option.getOptionText());
                    optionData.put("order", option.getOptionOrder());

                    options.add(optionData);
                }
            }

            options.sort(Comparator.comparing(o -> (Integer) o.get("order")));

            questionData.put("options", options);

            response.add(questionData);
        }

        response.sort(Comparator.comparing(q -> (Integer) q.get("order")));

        return response;
    }

    @PostMapping("/tasks/delete/{id}")
    public String deleteTask(@PathVariable Long id,
                             HttpSession session) {

        User user = (User) session.getAttribute("user");

        if (user == null) {
            return "redirect:/login";
        }

        if (!"DOCENTE".equals(user.getRole())) {
            return "redirect:/tasks";
        }

        taskService.deleteTask(id, user);

        return "redirect:/tasks";
    }

    @PostMapping("/tasks/submit/{id}")
    public String submitTask(@PathVariable Long id,
                             @RequestParam(required = false) String answerText,
                             @RequestParam(required = false) MultipartFile[] files,
                             HttpSession session) {

        User user = (User) session.getAttribute("user");

        if (user == null) {
            return "redirect:/login";
        }

        if ("DOCENTE".equals(user.getRole())) {
            return "redirect:/tasks";
        }

        submissionService.submitTask(id, user, answerText, files);

        return "redirect:/tasks";
    }

    @GetMapping("/tasks/practice/{id}")
    public String viewPractice(@PathVariable Long id,
                               Model model,
                               HttpSession session) {

        User user = (User) session.getAttribute("user");

        if (user == null) {
            return "redirect:/login";
        }

        Task task = taskService.getTaskById(id);

        if (task == null) {
            return "redirect:/tasks";
        }

        String primerNombre = user.getName().split(" ")[0];

        Submission submission =
                submissionService.getSubmissionByTaskAndStudent(task, user);

        model.addAttribute("nombre", primerNombre);
        model.addAttribute("task", task);
        model.addAttribute("questions", taskService.getPracticeQuestions(id));
        model.addAttribute("submission", submission);

        return "practice-details";
    }

    @PostMapping("/tasks/submit-practice/{id}")
    public String submitPractice(@PathVariable Long id,
                                 @RequestParam Map<String, String> answers,
                                 @RequestParam(required = false) Long courseId,
                                 @RequestParam(required = false)
                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                 LocalDateTime startedAt,
                                 @RequestParam(required = false)
                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                 LocalDateTime finishedAt,
                                 @RequestParam(required = false, defaultValue = "tasks") String source,
                                 HttpSession session) {

        User user = (User) session.getAttribute("user");

        if (user == null) {
            return "redirect:/login";
        }

        if ("DOCENTE".equals(user.getRole())) {
            return "redirect:/tasks";
        }

        submissionService.submitPracticeTask(id, user, answers);

        Task task = taskService.getTaskById(id);

        if (courseId == null && task != null && task.getCourse() != null) {
            courseId = task.getCourse().getId();
        }

        if (courseId != null && startedAt != null && finishedAt != null) {
            studyService.saveStudySession(user, courseId, id, startedAt, finishedAt);
        }

        if ("study".equals(source)) {
            return "redirect:/study";
        }

        return "redirect:/tasks";
    }

    @GetMapping("/tasks/submissions/{id}")
    public String viewSubmissions(@PathVariable Long id,
                                  Model model,
                                  HttpSession session) {

        User user = (User) session.getAttribute("user");

        if (user == null) {
            return "redirect:/login";
        }

        if (!"DOCENTE".equals(user.getRole())) {
            return "redirect:/tasks";
        }

        Task task = taskService.getTaskById(id);

        if (task == null) {
            return "redirect:/tasks";
        }

        String primerNombre = user.getName().split(" ")[0];

        List<Submission> submissions =
                submissionService.getSubmissionsByTask(id);

        Map<Long, List<Map<String, String>>> submissionFiles =
                new HashMap<>();

        for (Submission submission : submissions) {
            submissionFiles.put(
                    submission.getId(),
                    submissionService.getFilesFromSubmission(submission)
            );
        }

        model.addAttribute("nombre", primerNombre);
        model.addAttribute("task", task);
        model.addAttribute("submissions", submissions);
        model.addAttribute("submissionFiles", submissionFiles);
        model.addAttribute("pendingStudents", submissionService.getPendingStudentsByTask(id));
        model.addAttribute("questions", taskService.getPracticeQuestions(id));
        model.addAttribute("practiceAnswersMap", submissionService.getPracticeAnswersByTask(id));

        return "task-submissions";
    }

    @GetMapping("/tasks/download/{storedName}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String storedName) {

        try {
            Path filePath =
                    submissionService.getUploadPath().resolve(storedName).normalize();

            Resource resource =
                    new UrlResource(filePath.toUri());

            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            String fileName =
                    storedName.contains("_")
                            ? storedName.substring(storedName.indexOf("_") + 1)
                            : storedName;

            String encodedFileName =
                    URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                            .replace("+", "%20");

            return ResponseEntity.ok()
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename*=UTF-8''" + encodedFileName
                    )
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}