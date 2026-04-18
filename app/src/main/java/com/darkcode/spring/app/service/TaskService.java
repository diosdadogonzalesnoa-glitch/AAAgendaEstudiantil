package com.darkcode.spring.app.service;

import com.darkcode.spring.app.model.Task;
import com.darkcode.spring.app.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TaskService {

    @Autowired
    private TaskRepository repo;

    public Task save(Task task) {
        return repo.save(task);
    }

    public List<Task> list() {
        return repo.findAll();
    }
}