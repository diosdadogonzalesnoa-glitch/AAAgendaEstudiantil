package com.darkcode.spring.app.controller;

import com.darkcode.spring.app.model.Course;
import com.darkcode.spring.app.model.Task;
import com.darkcode.spring.app.model.User;
import com.darkcode.spring.app.service.CourseService;
import com.darkcode.spring.app.service.StudyService;
import com.darkcode.spring.app.service.SubmissionService;
import com.darkcode.spring.app.service.TaskService;
import com.darkcode.spring.app.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@Controller
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private CourseService courseService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private SubmissionService submissionService;

    @Autowired
    private StudyService studyService;

    @Autowired
    private AuthenticationManager authenticationManager;

    private Map<String, Integer> intentos = new HashMap<>();
    private Map<String, Long> bloqueos = new HashMap<>();

    private static final int MAX_INTENTOS = 3;
    private static final int BLOQUEO_MS = 10000;

    @GetMapping("/")
    public String root() {
        return "redirect:/login";
    }

    @GetMapping("/register")
    public String showRegister() {
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute User user,
                               @RequestParam String role,
                               Model model) {

        if (!user.getEmail().endsWith("@utp.edu.pe")) {
            model.addAttribute("error", "El correo debe ser @utp.edu.pe");
            return "register";
        }

        if (role.equals("ESTUDIANTE") &&
            !user.getEmail().startsWith("U")) {

            model.addAttribute("error",
                    "El correo de estudiante debe iniciar con U");

            return "register";
        }

        if (role.equals("DOCENTE") &&
            !user.getEmail().startsWith("C")) {

            model.addAttribute("error",
                    "El correo de docente debe iniciar con C");

            return "register";
        }

        if (userService.findByEmail(user.getEmail()) != null) {

            model.addAttribute("error",
                    "El correo ya está registrado");

            return "register";
        }

        user.setRole(role);

        userService.register(user);

        return "redirect:/login?msg=Registro exitoso. Ahora inicia sesión.";
    }

    @GetMapping("/login")
    public String showLogin(@RequestParam(required = false) String msg,
                            Model model) {

        if (msg != null) {
            model.addAttribute("msg", msg);
        }

        return "login";
    }

    @PostMapping("/login")
    public String loginUser(@RequestParam String email,
                            @RequestParam String password,
                            HttpSession session) {

        long ahora = System.currentTimeMillis();

        if (bloqueos.containsKey(email)) {

            long fin = bloqueos.get(email);

            if (ahora < fin) {

                long segundos = (fin - ahora) / 1000;

                return "redirect:/login?msg=Cuenta bloqueada (" +
                        segundos + "s)";

            } else {

                bloqueos.remove(email);
                intentos.put(email, MAX_INTENTOS);
            }
        }

        int restantes =
                intentos.getOrDefault(email, MAX_INTENTOS);

        User user = userService.findByEmail(email);

        if (user != null &&
            userService.rawPasswordMatches(user, password)) {

            userService.upgradePasswordToBCrypt(user, password);
        }

        try {

            Authentication authentication =
                    authenticationManager.authenticate(
                            new UsernamePasswordAuthenticationToken(
                                    email,
                                    password
                            )
                    );

            SecurityContextHolder.getContext()
                    .setAuthentication(authentication);

            session.setAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    SecurityContextHolder.getContext()
            );

            User loggedUser =
                    userService.findByEmail(email);

            intentos.put(email, MAX_INTENTOS);

            User sessionUser = new User();

            sessionUser.setId(loggedUser.getId());
            sessionUser.setName(loggedUser.getName());
            sessionUser.setEmail(loggedUser.getEmail());
            sessionUser.setRole(loggedUser.getRole());

            session.setAttribute("user", sessionUser);

            return "redirect:/dashboard";

        } catch (AuthenticationException e) {

            restantes--;

            intentos.put(email, restantes);

            if (restantes <= 0) {

                bloqueos.put(email, ahora + BLOQUEO_MS);

                return "redirect:/login?msg=Cuenta bloqueada (10s)";

            } else {

                return "redirect:/login?msg=Te quedan " +
                        restantes + " intentos";
            }
        }
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model,
                            HttpSession session) {

        User user =
                (User) session.getAttribute("user");

        if (user == null) {
            return "redirect:/login";
        }

        String primerNombre =
                user.getName().split(" ")[0];

        List<Course> courses;

        if ("DOCENTE".equals(user.getRole())) {
            courses = courseService.getCoursesByUser(user);
        } else {
            courses = courseService.getEnrolledCoursesByStudent(user);
        }

        List<Task> tasks = taskService.getTasksByUser(user);

        long totalTasks = taskService.totalTasks(user);
        long overdueTasks = taskService.overdueTasks(user);

        Set<Long> submittedTaskIds = new HashSet<>();

        if (!"DOCENTE".equals(user.getRole())) {
            submittedTaskIds = submissionService.getSubmittedTaskIds(user);
        }

        List<Task> dashboardTasks = new ArrayList<>();

        for (Task task : tasks) {
            if (dashboardTasks.size() < 6) {
                dashboardTasks.add(task);
            }
        }

        Map<Long, Integer> totalMinutesByCourse =
                studyService.getTotalMinutesByCourse(user);

        double weeklyStudyHours =
                studyService.getWeeklyHoursByStudent(user);

        List<Map<String, Object>> courseStudyChart = new ArrayList<>();

        for (Course course : courses) {

            int totalMinutes =
                    totalMinutesByCourse.getOrDefault(course.getId(), 0);

            double hours =
                    studyService.roundHours(totalMinutes);

            int heightPercent =
                    (int) Math.min(100, Math.round((hours / 8.0) * 100));

            Map<String, Object> data = new HashMap<>();

            data.put("courseId", course.getId());
            data.put("courseName", course.getName());
            data.put("acronym", buildCourseAcronym(course.getName()));
            data.put("hours", hours);
            data.put("heightPercent", heightPercent);

            courseStudyChart.add(data);
        }

        model.addAttribute("nombre", primerNombre);
        model.addAttribute("role", user.getRole());

        model.addAttribute("totalTasks", totalTasks);
        model.addAttribute("overdueTasks", overdueTasks);
        model.addAttribute("totalCourses", courses.size());
        model.addAttribute("todayReminders", 0);
        model.addAttribute("weeklyStudyHours", weeklyStudyHours);

        model.addAttribute("upcomingTasks", dashboardTasks);
        model.addAttribute("submittedTaskIds", submittedTaskIds);
        model.addAttribute("courseStudyChart", courseStudyChart);
        model.addAttribute("now", LocalDateTime.now());

        return "dashboard";
    }

    private String buildCourseAcronym(String courseName) {

        if (courseName == null || courseName.trim().isEmpty()) {
            return "SC";
        }

        String[] words = courseName.trim().split("\\s+");

        StringBuilder acronym = new StringBuilder();

        List<String> ignoredWords = Arrays.asList(
                "a", "al", "de", "del", "la", "las", "el", "los", "para", "y"
        );

        for (String word : words) {

            String clean = word.replaceAll("[^A-Za-zÁÉÍÓÚáéíóúÑñ0-9]", "");

            if (clean.isEmpty()) {
                continue;
            }

            if (clean.matches("\\d+")) {
                acronym.append(clean);
                continue;
            }

            if (ignoredWords.contains(clean.toLowerCase())) {
                continue;
            }

            acronym.append(clean.substring(0, 1).toUpperCase());
        }

        if (acronym.length() == 0) {
            return "SC";
        }

        return acronym.toString();
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {

        SecurityContextHolder.clearContext();

        session.invalidate();

        return "redirect:/login";
    }
}