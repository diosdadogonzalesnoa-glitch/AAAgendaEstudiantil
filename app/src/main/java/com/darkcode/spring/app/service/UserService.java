package com.darkcode.spring.app.service;

import com.darkcode.spring.app.model.User;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserService {

    private List<User> users = new ArrayList<>();
    private Long idCounter = 1L;

    //REGISTRAR
    public void register(User user) {
        user.setId(idCounter++);
        users.add(user);
    }

    //LOGIN
    public User findByEmailAndPassword(String email, String password) {
        for (User u : users) {
            if (u.getEmail().equals(email) && u.getPassword().equals(password)) {
                return u;
            }
        }
        return null;
    }

    //CRUD para POSTMAN

    // CREATE
    public User create(User user) {
        user.setId(idCounter++);
        users.add(user);
        return user;
    }

    // READ ALL
    public List<User> getAll() {
        return users;
    }

    // READ BY ID
    public User getById(Long id) {
        for (User u : users) {
            if (u.getId().equals(id)) {
                return u;
            }
        }
        return null;
    }

    // UPDATE
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

    // DELETE
    public boolean delete(Long id) {
        return users.removeIf(u -> u.getId().equals(id));
    }
}