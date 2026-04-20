package com.darkcode.spring.app.controller;

import com.darkcode.spring.app.model.User;
import com.darkcode.spring.app.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Controller
public class AuthController {

    @Autowired
    private UserService userService;

    private User usuarioLogueado;

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
    public String registerUser(@ModelAttribute User user) {
        userService.register(user);
        return "redirect:/login";
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
                return "redirect:/login?msg=Cuenta bloqueada (" + segundos + "s)";
            } else {
                bloqueos.remove(email);
                intentos.put(email, MAX_INTENTOS);
            }
        }

        int restantes = intentos.getOrDefault(email, MAX_INTENTOS);

        User user = userService.findByEmailAndPassword(email, password);

        if (user != null) {

            intentos.put(email, MAX_INTENTOS);

            usuarioLogueado = user;
            session.setAttribute("user", user);

            return "redirect:/dashboard";

        } else {

            restantes--;
            intentos.put(email, restantes);

            if (restantes <= 0) {
                bloqueos.put(email, ahora + BLOQUEO_MS);
                return "redirect:/login?msg=Cuenta bloqueada (10s)";
            } else {
                return "redirect:/login?msg=Te quedan " + restantes + " intentos";
            }
        }
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, HttpSession session) {

        User user = (User) session.getAttribute("user");

        if (user == null) {
            return "redirect:/login";
        }

        String primerNombre = user.getName().split(" ")[0];
        model.addAttribute("nombre", primerNombre);

        return "dashboard";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        usuarioLogueado = null;
        session.invalidate();
        return "redirect:/login";
    }
}