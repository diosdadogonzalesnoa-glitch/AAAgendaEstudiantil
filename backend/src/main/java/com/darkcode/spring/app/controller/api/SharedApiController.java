package com.darkcode.spring.app.controller.api;

import com.darkcode.spring.app.model.Task;
import com.darkcode.spring.app.model.User;
import com.darkcode.spring.app.service.CourseService;
import com.darkcode.spring.app.service.LessonService;
import com.darkcode.spring.app.service.ReminderService;
import com.darkcode.spring.app.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;

@RestController
public class SharedApiController {

    @Autowired private UserService userService;
    @Autowired private ReminderService reminderService;
    @Autowired private CourseService courseService;
    @Autowired private LessonService lessonService;

    private User u(Authentication auth) {
        return userService.findByEmail(auth.getName());
    }

    @GetMapping("/api/recordatorios")
    public ResponseEntity<?> recordatorios(Authentication auth) {
        User user = u(auth);
        if (user == null) return ResponseEntity.status(401).build();

        Map<String, List<Task>> grouped = reminderService.getGroupedTasks(user);

        Map<String, Object> res = new HashMap<>();
        res.put("todayTasks", grouped.get("today").stream().map(this::taskMap).toList());
        res.put("tomorrowTasks", grouped.get("tomorrow").stream().map(this::taskMap).toList());
        res.put("futureTasks", grouped.get("future").stream().map(this::taskMap).toList());
        res.put("pendingCount", reminderService.countPending(user));
        res.put("completedCount", reminderService.countCompleted(user));
        return ResponseEntity.ok(res);
    }

    @PostMapping("/api/perfil/actualizar")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, Object> body, Authentication auth) {
        User user = u(auth);
        if (user == null) return ResponseEntity.status(401).build();
        String name = body.get("name") != null ? body.get("name").toString().trim() : null;
        if (name == null || name.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "Nombre vacío"));
        user.setName(name);
        userService.update(user.getId(), user);
        return ResponseEntity.ok(Map.of("message", "Perfil actualizado"));
    }

    @GetMapping("/api/lessons/file/{storedName}")
    public ResponseEntity<Resource> lessonFile(@PathVariable String storedName, Authentication auth) {
        if (u(auth) == null) return ResponseEntity.status(401).build();
        try {
            Path filePath = lessonService.getUploadPath().resolve(storedName).normalize().toAbsolutePath();
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) return ResponseEntity.notFound().build();
            String fileName = storedName.contains("_") ? storedName.substring(storedName.indexOf("_") + 1) : storedName;
            String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + encoded)
                    .header(HttpHeaders.CONTENT_TYPE, "application/pdf")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    private Map<String, Object> taskMap(Task t) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", t.getId());
        m.put("title", t.getTitle());
        m.put("description", t.getDescription());
        m.put("dueDate", t.getDueDate());
        if (t.getCourse() != null)
            m.put("course", Map.of("id", t.getCourse().getId(), "name", t.getCourse().getName()));
        return m;
    }
}
