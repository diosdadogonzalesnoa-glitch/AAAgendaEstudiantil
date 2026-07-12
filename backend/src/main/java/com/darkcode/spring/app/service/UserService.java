package com.darkcode.spring.app.service;

import com.darkcode.spring.app.model.User;
import com.darkcode.spring.app.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public void register(User user) {

        if (!(user.getEmail().toLowerCase().endsWith("@utp.edu.pe")
      || user.getEmail().toLowerCase().endsWith("@gmail.com"))) {

    throw new RuntimeException(
            "Correo inválido. Debe ser @utp.edu.pe o @gmail.com"
    );
}

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        userRepository.save(user);
    }

    public User findByEmail(String email) {

        return userRepository.findByEmail(email);
    }

    public User findByEmailAndPassword(String email,
                                       String password) {

        User user = userRepository.findByEmail(email);

        if (user == null) {
            return null;
        }

        if (passwordEncoder.matches(password, user.getPassword())) {
            return user;
        }

        return null;
    }

    public boolean rawPasswordMatches(User user,
                                      String rawPassword) {

        if (user == null || rawPassword == null || user.getPassword() == null) {
            return false;
        }

        return user.getPassword().equals(rawPassword);
    }

    public User upgradePasswordToBCrypt(User user,
                                        String rawPassword) {

        if (user == null || rawPassword == null) {
            return null;
        }

        user.setPassword(passwordEncoder.encode(rawPassword));

        return userRepository.save(user);
    }

    public User updateProfile(Long id,
                              String name,
                              String email) {

        User user = userRepository
                .findById(id)
                .orElse(null);

        if (user != null) {

            if (!(email.toLowerCase().endsWith("@utp.edu.pe")
      || email.toLowerCase().endsWith("@gmail.com"))) {
    return null;
}

            user.setName(name);
            user.setEmail(email);

            return userRepository.save(user);
        }

        return null;
    }

    public User create(User user) {

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        return userRepository.save(user);
    }

    public List<User> getAll() {

        return userRepository.findAll();
    }

    public User getById(Long id) {

        return userRepository.findById(id).orElse(null);
    }

    public User update(Long id,
                       User newUser) {

        User user = userRepository
                .findById(id)
                .orElse(null);

        if (user != null) {

            user.setName(newUser.getName());
            user.setEmail(newUser.getEmail());

            if (newUser.getPassword() != null &&
                !newUser.getPassword().trim().isEmpty()) {

                user.setPassword(passwordEncoder.encode(newUser.getPassword()));
            }

            return userRepository.save(user);
        }

        return null;
    }

    public boolean delete(Long id) {

        if (userRepository.existsById(id)) {

            userRepository.deleteById(id);

            return true;
        }

        return false;
    }
}