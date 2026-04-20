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

    // 🔥 VER PERFIL
    @GetMapping("/profile")
    public String profile(Model model, HttpSession session) {

        User user = (User) session.getAttribute("user");

        if (user == null) {
            return "redirect:/login";
        }

        String nombreCompleto = user.getName();
        String primerNombre = nombreCompleto.split(" ")[0];

        model.addAttribute("nombre", primerNombre);
        model.addAttribute("email", user.getEmail());
        model.addAttribute("user", user);

        return "profile";
    }

    // 🔥 ACTUALIZAR PERFIL
    @PostMapping("/profile/update")
    public String updateProfile(@RequestParam String name,
                                @RequestParam String email,
                                HttpSession session,
                                Model model) {

        User user = (User) session.getAttribute("user");

        if (user == null) {
            return "redirect:/login";
        }

        User updated = userService.updateProfile(user.getId(), name, email);

        if (updated == null) {
            String primerNombre = user.getName().split(" ")[0];

            model.addAttribute("nombre", primerNombre);
            model.addAttribute("email", user.getEmail());
            model.addAttribute("error", "El correo debe terminar en @gmail.com");

            return "profile";
        }

        session.setAttribute("user", updated);

        return "redirect:/profile";
    }
}