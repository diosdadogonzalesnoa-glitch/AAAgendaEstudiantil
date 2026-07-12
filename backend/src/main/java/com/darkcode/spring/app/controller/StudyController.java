package com.darkcode.spring.app.controller;

import com.darkcode.spring.app.model.Course;
import com.darkcode.spring.app.model.PracticeQuestion;
import com.darkcode.spring.app.model.Task;
import com.darkcode.spring.app.model.User;
import com.darkcode.spring.app.service.CourseService;
import com.darkcode.spring.app.service.StudyService;
import com.darkcode.spring.app.service.SubmissionService;
import com.darkcode.spring.app.service.TaskService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.*;

@Controller
public class StudyController {

    @Autowired
    private CourseService courseService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private SubmissionService submissionService;

    @Autowired
    private StudyService studyService;

    @GetMapping("/study")
    public String study(Model model,
                        HttpSession session) {

        User user = (User) session.getAttribute("user");

        if (user == null) {
            return "redirect:/login";
        }

        String primerNombre = user.getName().split(" ")[0];

        model.addAttribute("nombre", primerNombre);
        model.addAttribute("role", user.getRole());

        if ("DOCENTE".equals(user.getRole())) {

            List<Course> teacherCourses =
                    courseService.getCoursesByUser(user);

            model.addAttribute("courses", teacherCourses);
            model.addAttribute("pendingPracticeTasks", new ArrayList<Task>());
            model.addAttribute("questionsByTask", new HashMap<Long, List<PracticeQuestion>>());
            model.addAttribute("weeklyStudyHours", 0);
            model.addAttribute("totalSessions", 0);
            model.addAttribute("weeklyHoursByDay", new LinkedHashMap<String, Double>());
            model.addAttribute("totalMinutesByCourse", new HashMap<Long, Integer>());
            model.addAttribute("totalSessionsByCourse", new HashMap<Long, Long>());
            model.addAttribute("courseStats", new ArrayList<>());
            model.addAttribute("teacherCourseStudentStats",
                    studyService.getTeacherCourseStudentStats(teacherCourses));

            return "study";
        }

        List<Course> courses =
                courseService.getEnrolledCoursesByStudent(user);

        List<Task> allTasks =
                taskService.getTasksByUser(user);

        Set<Long> submittedTaskIds =
                submissionService.getSubmittedTaskIds(user);

        List<Task> pendingPracticeTasks = new ArrayList<>();

        Map<Long, List<PracticeQuestion>> questionsByTask = new HashMap<>();

        for (Task task : allTasks) {

            if (task == null || task.getId() == null) {
                continue;
            }

            boolean isPractice =
                    "PRACTICA".equals(task.getTaskType());

            boolean alreadySubmitted =
                    submittedTaskIds.contains(task.getId());

            if (isPractice && !alreadySubmitted) {
                pendingPracticeTasks.add(task);
                questionsByTask.put(
                        task.getId(),
                        taskService.getPracticeQuestions(task.getId())
                );
            }
        }

        model.addAttribute("courses", courses);
        model.addAttribute("pendingPracticeTasks", pendingPracticeTasks);
        model.addAttribute("questionsByTask", questionsByTask);
        model.addAttribute("weeklyStudyHours",
                studyService.getWeeklyHoursByStudent(user));
        model.addAttribute("totalSessions",
                studyService.getTotalSessionsByStudent(user));
        model.addAttribute("weeklyHoursByDay",
                studyService.getWeeklyHoursByDay(user));
        model.addAttribute("totalMinutesByCourse",
                studyService.getTotalMinutesByCourse(user));
        model.addAttribute("totalSessionsByCourse",
                studyService.getTotalSessionsByCourse(user));
        model.addAttribute("courseStats",
                studyService.getCourseStatsForStudent(user, courses));
        model.addAttribute("teacherCourseStudentStats", new HashMap<>());

        return "study";
    }
}
