package com.darkcode.spring.app.controller;

import com.darkcode.spring.app.model.User;
import com.darkcode.spring.app.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {

    @Autowired
    private UserService userService;

    private User usuarioLogueado; //guarda usuario de memoria

    //ir a login automáticamente
    @GetMapping("/")
    public String root() {
        return "redirect:/login";
    }

    //mostrar registro
    @GetMapping("/register")
    public String showRegister() {
        return "register";
    }

    //procesar registro
    @PostMapping("/register")
    public String registerUser(@ModelAttribute User user) {
        userService.register(user);
        return "redirect:/login";
    }

    //mostrar login
    @GetMapping("/login")
    public String showLogin() {
        return "login";
    }

    //procesar login
    @PostMapping("/login")
    public String loginUser(@RequestParam String email,
                            @RequestParam String password) {

        User user = userService.findByEmailAndPassword(email, password);

        if (user != null) {
            usuarioLogueado = user;
            return "redirect:/dashboard";
        } else {
            return "redirect:/login?error=true";
        }
    }

    //dashboard
    @GetMapping("/dashboard")
    public String dashboard(Model model) {

        if (usuarioLogueado == null) {
            return "redirect:/login";
        }

        String nombreCompleto = usuarioLogueado.getName();
        String primerNombre = nombreCompleto.split(" ")[0];

        model.addAttribute("nombre", primerNombre);

        return "dashboard";
    }

    //cerrar sesión
    @GetMapping("/logout")
    public String logout() {
        usuarioLogueado = null;
        return "redirect:/login";
    }
}