package com.darkcode.spring.app.model;

import java.util.ArrayList;
import java.util.List;

public class User {

    private Long id;
    private String name;
    private String email;
    private String password;

    // 🔥 cursos del usuario
    private List<Course> courses = new ArrayList<>();

    public Long getId() { 
        return id; 
    }

    public void setId(Long id) { 
        this.id = id; 
    }

    public String getName() { 
        return name; 
    }

    public void setName(String name) { 
        this.name = name; 
    }

    public String getEmail() { 
        return email; 
    }

    public void setEmail(String email) { 
        this.email = email; 
    }

    public String getPassword() { 
        return password; 
    }

    public void setPassword(String password) { 
        this.password = password; 
    }

    public List<Course> getCourses() { 
        return courses; 
    }

    public void setCourses(List<Course> courses) { 
        this.courses = courses; 
    }
}