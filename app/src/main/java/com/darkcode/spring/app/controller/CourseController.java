package com.darkcode.spring.app.controller;

import com.darkcode.spring.app.model.Course;
import com.darkcode.spring.app.model.Lesson;
import com.darkcode.spring.app.model.Submission;
import com.darkcode.spring.app.model.Task;
import com.darkcode.spring.app.model.User;
import com.darkcode.spring.app.service.CourseService;
import com.darkcode.spring.app.service.LessonService;
import com.darkcode.spring.app.service.SubmissionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@Controller
public class CourseController {

    @Autowired
    private CourseService courseService;

    @Autowired
    private SubmissionService submissionService;

    @Autowired
    private LessonService lessonService;

    @GetMapping("/courses")
    public String courses(
            @RequestParam(required = false) String search,
            Model model,
            HttpSession session) {

        User user = (User) session.getAttribute("user");

        if (user == null) {
            return "redirect:/login";
        }

        String primerNombre = user.getName().split(" ")[0];

        model.addAttribute("nombre", primerNombre);
        model.addAttribute("search", search);
        model.addAttribute("role", user.getRole());

        if ("DOCENTE".equals(user.getRole())) {

            model.addAttribute("courses",
                    courseService.getCoursesByUser(user));

            model.addAttribute("searchMode", false);

        } else {

            if (search != null && !search.trim().isEmpty()) {

                model.addAttribute("courses",
                        courseService.searchCourses(search));

                model.addAttribute("searchMode", true);

            } else {

                model.addAttribute("courses",
                        courseService.getEnrolledCoursesByStudent(user));

                model.addAttribute("searchMode", false);
            }
        }

        return "courses";
    }

    @PostMapping("/courses/add")
    public String addCourse(@ModelAttribute Course course,
                            HttpSession session) {

        User user = (User) session.getAttribute("user");

        if (user == null) {
            return "redirect:/login";
        }

        courseService.addCourse(course, user);

        return "redirect:/courses";
    }

    @GetMapping("/courses/enroll/{id}")
    public String enrollCourse(@PathVariable Long id,
                               HttpSession session) {

        User user = (User) session.getAttribute("user");

        if (user == null) {
            return "redirect:/login";
        }

        courseService.enrollStudent(user, id);

        return "redirect:/courses";
    }

    @GetMapping("/courses/{id}")
    public String courseDetails(@PathVariable Long id,
                                Model model,
                                HttpSession session) {

        User user = (User) session.getAttribute("user");

        if (user == null) {
            return "redirect:/login";
        }

        Course course = courseService.getCourseById(id);

        if (course == null) {
            return "redirect:/courses";
        }

        String primerNombre = user.getName().split(" ")[0];

        Map<Long, String> taskStatus = new HashMap<>();
        Map<Long, Integer> taskGrades = new HashMap<>();

        if ("ESTUDIANTE".equals(user.getRole())) {

            for (Task task : course.getTasks()) {

                Submission submission =
                        submissionService.getSubmissionByTaskAndStudent(task, user);

                if (submission == null) {
                    taskStatus.put(task.getId(), "NO_ENTREGADO");
                } else if (submission.getGrade() == null) {
                    taskStatus.put(task.getId(), "PENDIENTE_CALIFICACION");
                } else {
                    taskStatus.put(task.getId(), "CALIFICADO");
                    taskGrades.put(task.getId(), submission.getGrade());
                }
            }
        }

        model.addAttribute("nombre", primerNombre);
        model.addAttribute("course", course);
        model.addAttribute("tasks", course.getTasks());
        model.addAttribute("taskStatus", taskStatus);
        model.addAttribute("taskGrades", taskGrades);
        model.addAttribute("lessonGroups",
                lessonService.getLessonGroupsByCourse(id));

        return "course-details";
    }

    @PostMapping("/courses/{courseId}/lessons/add")
    public String addLesson(@PathVariable Long courseId,
                            @RequestParam String title,
                            @RequestParam(required = false) String description,
                            @RequestParam MultipartFile pdfFile,
                            HttpSession session) {

        User user = (User) session.getAttribute("user");

        if (user == null) {
            return "redirect:/login";
        }

        if (!"DOCENTE".equals(user.getRole())) {
            return "redirect:/courses/" + courseId;
        }

        lessonService.addLesson(courseId, user, title, description, pdfFile);

        return "redirect:/courses/" + courseId;
    }

    @GetMapping("/courses/{courseId}/lessons/{lessonId}")
    public String viewLesson(@PathVariable Long courseId,
                             @PathVariable Long lessonId,
                             Model model,
                             HttpSession session) {

        User user = (User) session.getAttribute("user");

        if (user == null) {
            return "redirect:/login";
        }

        Course course = courseService.getCourseById(courseId);

        if (course == null) {
            return "redirect:/courses";
        }

        Lesson lesson = lessonService.getLessonById(lessonId);

        if (lesson == null) {
            return "redirect:/courses/" + courseId;
        }

        String primerNombre = user.getName().split(" ")[0];

        model.addAttribute("nombre", primerNombre);
        model.addAttribute("course", course);
        model.addAttribute("lesson", lesson);

        return "lesson-view";
    }

    @PostMapping("/courses/{courseId}/lessons/delete/{lessonId}")
    public String deleteLesson(@PathVariable Long courseId,
                               @PathVariable Long lessonId,
                               HttpSession session) {

        User user = (User) session.getAttribute("user");

        if (user == null) {
            return "redirect:/login";
        }

        if (!"DOCENTE".equals(user.getRole())) {
            return "redirect:/courses/" + courseId;
        }

        lessonService.deleteLesson(lessonId, user);

        return "redirect:/courses/" + courseId;
    }

    @GetMapping("/courses/lessons/file/{storedName}")
    public ResponseEntity<Resource> viewLessonPdf(@PathVariable String storedName) {

        try {
            Path filePath =
                    lessonService.getUploadPath()
                            .resolve(storedName)
                            .normalize()
                            .toAbsolutePath();

            Resource resource =
                    new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
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
                            "inline; filename*=UTF-8''" + encodedFileName
                    )
                    .header(HttpHeaders.CONTENT_TYPE, "application/pdf")
                    .header("X-Content-Type-Options", "nosniff")
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/courses/update")
    public String updateCourse(@RequestParam Long id,
                               @RequestParam String name,
                               @RequestParam String teacher,
                               @RequestParam String schedule,
                               HttpSession session) {

        User user = (User) session.getAttribute("user");

        if (user == null) {
            return "redirect:/login";
        }

        if (!"DOCENTE".equals(user.getRole())) {
            return "redirect:/courses";
        }

        courseService.updateCourse(id, name, teacher, schedule);

        return "redirect:/courses";
    }

    @GetMapping("/courses/delete/{id}")
    public String deleteCourse(@PathVariable Long id,
                               HttpSession session) {

        User user = (User) session.getAttribute("user");

        if (user == null) {
            return "redirect:/login";
        }

        if (!"DOCENTE".equals(user.getRole())) {
            return "redirect:/courses";
        }

        courseService.deleteCourse(id, user);

        return "redirect:/courses";
    }
}