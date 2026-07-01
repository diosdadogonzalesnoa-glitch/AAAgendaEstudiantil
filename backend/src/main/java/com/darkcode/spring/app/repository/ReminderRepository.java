package com.darkcode.spring.app.repository;

import com.darkcode.spring.app.model.Reminder;
import com.darkcode.spring.app.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReminderRepository extends JpaRepository<Reminder, Long> {

    List<Reminder> findByUserOrderByReminderDateAsc(User user);

}