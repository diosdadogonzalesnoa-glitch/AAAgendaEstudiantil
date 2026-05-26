package com.darkcode.spring.app.controller;

import com.darkcode.spring.app.model.Course;
import com.darkcode.spring.app.model.User;
import com.darkcode.spring.app.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    // CREATE
    @PostMapping
    public User createUser(@RequestBody User user) {
        return userService.create(user);
    }

    // READ ALL
    @GetMapping
    public List<Map<String, Object>> getAllUsers() {

        List<User> users = userService.getAll();

        List<Map<String, Object>> response = new ArrayList<>();

        for (User user : users) {

            Map<String, Object> userMap = new HashMap<>();

            userMap.put("id", user.getId());
            userMap.put("name", user.getName());
            userMap.put("email", user.getEmail());
            userMap.put("password", user.getPassword());
            userMap.put("role", user.getRole());

            // CURSOS CREADOS
            List<Map<String, Object>> createdCourses = new ArrayList<>();

            for (Course course : user.getCourses()) {

                Map<String, Object> c = new HashMap<>();

                c.put("id", course.getId());
                c.put("name", course.getName());
                c.put("teacher", course.getTeacher());
                c.put("schedule", course.getSchedule());

                createdCourses.add(c);
            }

            // CURSOS INSCRITOS
            List<Map<String, Object>> enrolledCourses = new ArrayList<>();

            for (Course course : user.getEnrolledCourses()) {

                Map<String, Object> c = new HashMap<>();

                c.put("id", course.getId());
                c.put("name", course.getName());
                c.put("teacher", course.getTeacher());
                c.put("schedule", course.getSchedule());

                enrolledCourses.add(c);
            }

            userMap.put("courses", createdCourses);
            userMap.put("enrolledCourses", enrolledCourses);

            response.add(userMap);
        }

        return response;
    }

    // READ BY ID
    @GetMapping("/{id}")
    public Map<String, Object> getUser(@PathVariable Long id) {

        User user = userService.getById(id);

        if (user == null) {
            return null;
        }

        Map<String, Object> userMap = new HashMap<>();

        userMap.put("id", user.getId());
        userMap.put("name", user.getName());
        userMap.put("email", user.getEmail());
        userMap.put("password", user.getPassword());
        userMap.put("role", user.getRole());

        // CURSOS CREADOS
        List<Map<String, Object>> createdCourses = new ArrayList<>();

        for (Course course : user.getCourses()) {

            Map<String, Object> c = new HashMap<>();

            c.put("id", course.getId());
            c.put("name", course.getName());
            c.put("teacher", course.getTeacher());
            c.put("schedule", course.getSchedule());

            createdCourses.add(c);
        }

        // CURSOS INSCRITOS
        List<Map<String, Object>> enrolledCourses = new ArrayList<>();

        for (Course course : user.getEnrolledCourses()) {

            Map<String, Object> c = new HashMap<>();

            c.put("id", course.getId());
            c.put("name", course.getName());
            c.put("teacher", course.getTeacher());
            c.put("schedule", course.getSchedule());

            enrolledCourses.add(c);
        }

        userMap.put("courses", createdCourses);
        userMap.put("enrolledCourses", enrolledCourses);

        return userMap;
    }

    // UPDATE
    @PutMapping("/{id}")
    public User updateUser(@PathVariable Long id,
                           @RequestBody User user) {

        return userService.update(id, user);
    }

    // DELETE
    @DeleteMapping("/{id}")
    public String deleteUser(@PathVariable Long id) {

        boolean deleted = userService.delete(id);

        return deleted
                ? "Usuario eliminado"
                : "Usuario no encontrado";
    }
}