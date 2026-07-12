package com.darkcode.spring.app.controller;

import com.darkcode.spring.app.model.User;
import com.darkcode.spring.app.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class ProfileController {

    @Autowired
    private UserService userService;

    @GetMapping("/profile")
    public String profile(Model model, HttpSession session) {

        User user = (User) session.getAttribute("user");

        if (user == null) {
            return "redirect:/login";
        }

        String primerNombre = user.getName().split(" ")[0];

        model.addAttribute("nombre", primerNombre);
        model.addAttribute("email", user.getEmail());
        model.addAttribute("user", user);
        model.addAttribute("role", user.getRole());

        return "profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@RequestParam String name,
                                HttpSession session) {

        User user = (User) session.getAttribute("user");

        if (user == null) {
            return "redirect:/login";
        }

        user.setName(name);

        userService.update(user.getId(), user);

        session.setAttribute("user", user);

        return "redirect:/profile";
    }
}