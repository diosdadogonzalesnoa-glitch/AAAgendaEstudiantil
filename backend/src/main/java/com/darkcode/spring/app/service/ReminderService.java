package com.darkcode.spring.app.service;

import com.darkcode.spring.app.model.Course;
import com.darkcode.spring.app.model.Task;
import com.darkcode.spring.app.model.User;
import com.darkcode.spring.app.repository.CourseRepository;
import com.darkcode.spring.app.repository.SubmissionRepository;
import com.darkcode.spring.app.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ReminderService {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    public List<Task> getUserTasks(User sessionUser) {

        User user =
                userRepository.findById(sessionUser.getId())
                        .orElse(null);

        if (user == null) {
            return new ArrayList<>();
        }

        List<Task> tasks = new ArrayList<>();

        if ("DOCENTE".equals(user.getRole())) {

            List<Course> courses =
                    courseRepository.findByUserId(user.getId());

            for (Course course : courses) {
                tasks.addAll(course.getTasks());
            }

        } else {

            for (Course course : user.getEnrolledCourses()) {

                for (Task task : course.getTasks()) {

                    boolean submitted =
                            submissionRepository
                                    .findByTaskAndStudent(task, user)
                                    != null;

                    if (!submitted &&
                            task.getDueDate() != null &&
                            task.getDueDate().isAfter(LocalDateTime.now())) {

                        tasks.add(task);
                    }
                }
            }
        }

        tasks.sort(
                Comparator.comparing(Task::getDueDate)
        );

        return tasks;
    }

    public Map<String, List<Task>> getGroupedTasks(User user) {

        List<Task> tasks = getUserTasks(user);

        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        Map<String, List<Task>> grouped = new HashMap<>();

        grouped.put("today", new ArrayList<>());
        grouped.put("tomorrow", new ArrayList<>());
        grouped.put("future", new ArrayList<>());

        for (Task task : tasks) {

            LocalDate taskDate =
                    task.getDueDate().toLocalDate();

            if (taskDate.equals(today)) {

                grouped.get("today").add(task);

            } else if (taskDate.equals(tomorrow)) {

                grouped.get("tomorrow").add(task);

            } else if (taskDate.isAfter(tomorrow)) {

                grouped.get("future").add(task);
            }
        }

        return grouped;
    }

    public long countPending(User user) {
        return getUserTasks(user).size();
    }

    public long countCompleted(User sessionUser) {

        User user =
                userRepository.findById(sessionUser.getId())
                        .orElse(null);

        if (user == null) {
            return 0;
        }

        long completed = 0;

        for (Course course : user.getEnrolledCourses()) {

            for (Task task : course.getTasks()) {

                if (submissionRepository
                        .findByTaskAndStudent(task, user) != null) {

                    completed++;
                }
            }
        }

        return completed;
    }
}