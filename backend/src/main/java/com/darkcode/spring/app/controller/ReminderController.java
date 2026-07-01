package com.darkcode.spring.app.controller;

import com.darkcode.spring.app.model.Task;
import com.darkcode.spring.app.model.User;
import com.darkcode.spring.app.service.ReminderService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

@Controller
public class ReminderController {

    @Autowired
    private ReminderService reminderService;

    @GetMapping("/reminders")
    public String reminders(Model model,
                            HttpSession session) {

        User user =
                (User) session.getAttribute("user");

        if (user == null) {
            return "redirect:/login";
        }

        String nombre =
                user.getName().split(" ")[0];

        Map<String, List<Task>> grouped =
                reminderService.getGroupedTasks(user);

        model.addAttribute(
                "nombre",
                nombre
        );

        model.addAttribute(
                "todayTasks",
                grouped.get("today")
        );

        model.addAttribute(
                "tomorrowTasks",
                grouped.get("tomorrow")
        );

        model.addAttribute(
                "futureTasks",
                grouped.get("future")
        );

        model.addAttribute(
                "pendingCount",
                reminderService.countPending(user)
        );

        model.addAttribute(
                "completedCount",
                reminderService.countCompleted(user)
        );

        return "reminders";
    }
}