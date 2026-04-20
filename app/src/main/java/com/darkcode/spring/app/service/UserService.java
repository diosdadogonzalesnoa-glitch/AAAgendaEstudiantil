package com.darkcode.spring.app.service;

import com.darkcode.spring.app.model.User;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserService {

    private List<User> users = new ArrayList<>();
    private Long idCounter = 1L;

    // REGISTRAR
    public void register(User user) {
        user.setId(idCounter++);
        users.add(user);
    }

    // LOGIN
    public User findByEmailAndPassword(String email, String password) {
        for (User u : users) {
            if (u.getEmail().equals(email) && u.getPassword().equals(password)) {
                return u;
            }
        }
        return null;
    }

    // 🔥 NUEVO: actualizar perfil (SOLO nombre y email)
    public User updateProfile(Long id, String name, String email) {

        for (User u : users) {
            if (u.getId().equals(id)) {

                // validar gmail
                if (!email.endsWith("@gmail.com")) {
                    return null; // error de validación
                }

                u.setName(name);
                u.setEmail(email);

                return u;
            }
        }

        return null;
    }

    // CRUD para POSTMAN

    public User create(User user) {
        user.setId(idCounter++);
        users.add(user);
        return user;
    }

    public List<User> getAll() {
        return users;
    }

    public User getById(Long id) {
        for (User u : users) {
            if (u.getId().equals(id)) {
                return u;
            }
        }
        return null;
    }

    public User update(Long id, User newUser) {
        for (User u : users) {
            if (u.getId().equals(id)) {
                u.setName(newUser.getName());
                u.setEmail(newUser.getEmail());
                u.setPassword(newUser.getPassword());
                return u;
            }
        }
        return null;
    }

    public boolean delete(Long id) {
        return users.removeIf(u -> u.getId().equals(id));
    }
}