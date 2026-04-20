package com.darkcode.spring.app.controller;

import com.darkcode.spring.app.model.Course;
import com.darkcode.spring.app.model.User;
import com.darkcode.spring.app.service.CourseService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class CourseController {

    @Autowired
    private CourseService courseService;

    @GetMapping("/courses")
    public String courses(Model model, HttpSession session) {

        User user = (User) session.getAttribute("user");

        if (user == null) {
            return "redirect:/login";
        }

        // 🔥 SOLO PRIMER NOMBRE (igual que dashboard)
        String nombreCompleto = user.getName();
        String primerNombre = nombreCompleto.split(" ")[0];

        model.addAttribute("nombre", primerNombre);

        model.addAttribute("courses", courseService.getCoursesByUser(user));

        return "courses";
    }

    @PostMapping("/courses/add")
    public String addCourse(@ModelAttribute Course course, HttpSession session) {

        User user = (User) session.getAttribute("user");

        if (user == null) {
            return "redirect:/login";
        }

        course.setUser(user);

        courseService.addCourse(course, user);

        return "redirect:/courses";
    }
}