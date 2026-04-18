package com.darkcode.spring.app.controller;

import com.darkcode.spring.app.model.User;
import com.darkcode.spring.app.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public List<User> getAllUsers() {
        return userService.getAll();
    }

    // READ BY ID
    @GetMapping("/{id}")
    public User getUser(@PathVariable Long id) {
        return userService.getById(id);
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
        return deleted ? "Usuario eliminado" : "Usuario no encontrado";
    }
}