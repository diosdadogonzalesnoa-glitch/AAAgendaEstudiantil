package com.darkcode.spring.app.controller.api;

import com.darkcode.spring.app.config.JwtUtil;
import com.darkcode.spring.app.model.User;
import com.darkcode.spring.app.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class ApiAuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    private final Map<String, Integer> intentos = new HashMap<>();
    private final Map<String, Long> bloqueos = new HashMap<>();
    private static final int MAX_INTENTOS = 3;
    private static final long BLOQUEO_MS = 10000;

    public ApiAuthController(UserService userService,
                             AuthenticationManager authenticationManager,
                             JwtUtil jwtUtil) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String password = body.get("password");

        long ahora = System.currentTimeMillis();

        if (bloqueos.containsKey(email)) {
            long fin = bloqueos.get(email);
            if (ahora < fin) {
                long segundos = (fin - ahora) / 1000;
                return ResponseEntity.status(429).body(Map.of("error", "Cuenta bloqueada. Espera " + segundos + "s"));
            } else {
                bloqueos.remove(email);
                intentos.put(email, MAX_INTENTOS);
            }
        }

        User user = userService.findByEmail(email);
        if (user != null && userService.rawPasswordMatches(user, password)) {
            userService.upgradePasswordToBCrypt(user, password);
        }

        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));

            intentos.put(email, MAX_INTENTOS);
            String token = jwtUtil.generateToken(email, user != null ? user.getRole() : "");

            User found = userService.findByEmail(email);
            Map<String, Object> resp = new HashMap<>();
            resp.put("token", token);
            resp.put("id", found.getId());
            resp.put("name", found.getName());
            resp.put("email", found.getEmail());
            resp.put("role", found.getRole());

            return ResponseEntity.ok(resp);

        } catch (AuthenticationException e) {
            int restantes = intentos.getOrDefault(email, MAX_INTENTOS) - 1;
            intentos.put(email, restantes);
            if (restantes <= 0) {
                bloqueos.put(email, ahora + BLOQUEO_MS);
                return ResponseEntity.status(401).body(Map.of("error", "Cuenta bloqueada (10s)"));
            }
            return ResponseEntity.status(401).body(Map.of("error", "Credenciales incorrectas. Te quedan " + restantes + " intentos"));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        String email = body.get("email");
        String password = body.get("password");
        String role = body.get("role");

        if (email == null || !email.endsWith("@utp.edu.pe")) {
            return ResponseEntity.badRequest().body(Map.of("error", "El correo debe ser @utp.edu.pe"));
        }
        if ("ESTUDIANTE".equals(role) && !email.startsWith("U")) {
            return ResponseEntity.badRequest().body(Map.of("error", "El correo de estudiante debe iniciar con U"));
        }
        if ("DOCENTE".equals(role) && !email.startsWith("C")) {
            return ResponseEntity.badRequest().body(Map.of("error", "El correo de docente debe iniciar con C"));
        }
        if (userService.findByEmail(email) != null) {
            return ResponseEntity.badRequest().body(Map.of("error", "El correo ya está registrado"));
        }

        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(password);
        user.setRole(role);
        userService.register(user);

        return ResponseEntity.ok(Map.of("message", "Registro exitoso"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        String email = jwtUtil.extractEmail(token);
        User user = userService.findByEmail(email);
        if (user == null) return ResponseEntity.notFound().build();
        Map<String, Object> resp = new HashMap<>();
        resp.put("id", user.getId());
        resp.put("name", user.getName());
        resp.put("email", user.getEmail());
        resp.put("role", user.getRole());
        return ResponseEntity.ok(resp);
    }
}
