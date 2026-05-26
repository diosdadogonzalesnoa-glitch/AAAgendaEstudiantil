package com.darkcode.spring.app.service;

import com.darkcode.spring.app.model.Course;
import com.darkcode.spring.app.model.StudySession;
import com.darkcode.spring.app.model.Task;
import com.darkcode.spring.app.model.User;
import com.darkcode.spring.app.repository.CourseRepository;
import com.darkcode.spring.app.repository.StudySessionRepository;
import com.darkcode.spring.app.repository.TaskRepository;
import com.darkcode.spring.app.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class StudyService {

    @Autowired
    private StudySessionRepository repo;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private TaskRepository taskRepository;

    public StudySession save(StudySession s) {
        return repo.save(s);
    }

    public List<StudySession> list() {
        return repo.findAll();
    }

    @Transactional
    public StudySession saveStudySession(User sessionUser,
                                         Long courseId,
                                         Long taskId,
                                         LocalDateTime startedAt,
                                         LocalDateTime finishedAt) {

        User student = userRepository.findById(sessionUser.getId()).orElse(null);
        Course course = courseRepository.findById(courseId).orElse(null);
        Task task = taskRepository.findById(taskId).orElse(null);

        if (student == null || course == null || task == null || startedAt == null || finishedAt == null) {
            return null;
        }

        long minutes = Duration.between(startedAt, finishedAt).toMinutes();

        if (minutes <= 0) {
            minutes = 1;
        }

        StudySession studySession = new StudySession();
        studySession.setStudent(student);
        studySession.setCourse(course);
        studySession.setTask(task);
        studySession.setStartedAt(startedAt);
        studySession.setFinishedAt(finishedAt);
        studySession.setMinutes((int) minutes);

        return repo.save(studySession);
    }

    @Transactional
    public List<StudySession> getSessionsByStudent(User sessionUser) {

        User student = userRepository.findById(sessionUser.getId()).orElse(null);

        if (student == null) {
            return new ArrayList<>();
        }

        return repo.findByStudent(student);
    }

    @Transactional
    public double getTotalHoursByStudent(User sessionUser) {

        List<StudySession> sessions = getSessionsByStudent(sessionUser);

        int totalMinutes = 0;

        for (StudySession session : sessions) {
            totalMinutes += session.getMinutes();
        }

        return roundHours(totalMinutes);
    }

    @Transactional
    public long getTotalSessionsByStudent(User sessionUser) {
        return getSessionsByStudent(sessionUser).size();
    }

    @Transactional
    public double getWeeklyHoursByStudent(User sessionUser) {

        List<StudySession> sessions = getSessionsByStudent(sessionUser);

        int totalMinutes = getWeeklyMinutes(sessions);

        return roundHours(totalMinutes);
    }

    @Transactional
    public Map<Long, Integer> getTotalMinutesByCourse(User sessionUser) {

        List<StudySession> sessions = getSessionsByStudent(sessionUser);

        Map<Long, Integer> result = new HashMap<>();

        for (StudySession session : sessions) {

            if (session.getCourse() == null || session.getCourse().getId() == null) {
                continue;
            }

            Long courseId = session.getCourse().getId();

            result.put(courseId, result.getOrDefault(courseId, 0) + session.getMinutes());
        }

        return result;
    }

    @Transactional
    public Map<Long, Long> getTotalSessionsByCourse(User sessionUser) {

        List<StudySession> sessions = getSessionsByStudent(sessionUser);

        Map<Long, Long> result = new HashMap<>();

        for (StudySession session : sessions) {

            if (session.getCourse() == null || session.getCourse().getId() == null) {
                continue;
            }

            Long courseId = session.getCourse().getId();

            result.put(courseId, result.getOrDefault(courseId, 0L) + 1);
        }

        return result;
    }

    @Transactional
    public Map<String, Double> getWeeklyHoursByDay(User sessionUser) {

        List<StudySession> sessions = getSessionsByStudent(sessionUser);

        Map<String, Integer> minutesByDay = new LinkedHashMap<>();
        minutesByDay.put("lun", 0);
        minutesByDay.put("mar", 0);
        minutesByDay.put("mié", 0);
        minutesByDay.put("jue", 0);
        minutesByDay.put("vie", 0);
        minutesByDay.put("sáb", 0);
        minutesByDay.put("dom", 0);

        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(DayOfWeek.MONDAY);
        LocalDate sunday = today.with(DayOfWeek.SUNDAY);

        for (StudySession session : sessions) {

            if (session.getFinishedAt() == null) {
                continue;
            }

            LocalDate date = session.getFinishedAt().toLocalDate();

            if (date.isBefore(monday) || date.isAfter(sunday)) {
                continue;
            }

            String key = getDayKey(session.getFinishedAt().getDayOfWeek());

            minutesByDay.put(key, minutesByDay.get(key) + session.getMinutes());
        }

        Map<String, Double> result = new LinkedHashMap<>();

        for (Map.Entry<String, Integer> entry : minutesByDay.entrySet()) {
            result.put(entry.getKey(), roundHours(entry.getValue()));
        }

        return result;
    }

    @Transactional
    public List<Map<String, Object>> getCourseStatsForStudent(User sessionUser,
                                                              List<Course> courses) {

        List<Map<String, Object>> result = new ArrayList<>();

        Map<Long, Integer> totalMinutesByCourse = getTotalMinutesByCourse(sessionUser);
        Map<Long, Long> totalSessionsByCourse = getTotalSessionsByCourse(sessionUser);

        int index = 0;

        for (Course course : courses) {

            int totalMinutes = totalMinutesByCourse.getOrDefault(course.getId(), 0);

            int weeklyMinutes = getWeeklyMinutesByStudentAndCourse(sessionUser, course);

            double totalHours = roundHours(totalMinutes);
            double weeklyHours = roundHours(weeklyMinutes);
            double averageDaily = Math.round((weeklyHours / 7.0) * 10.0) / 10.0;
            long sessions = totalSessionsByCourse.getOrDefault(course.getId(), 0L);

            Map<String, Object> data = new HashMap<>();

            data.put("courseId", course.getId());
            data.put("courseName", course.getName());
            data.put("color", getColorByIndex(index));
            data.put("totalHours", totalHours);
            data.put("weeklyHours", weeklyHours);
            data.put("averageDaily", averageDaily);
            data.put("sessions", sessions);

            result.add(data);

            index++;
        }

        return result;
    }

    @Transactional
    public Map<Long, List<Map<String, Object>>> getTeacherCourseStudentStats(List<Course> courses) {

        Map<Long, List<Map<String, Object>>> result = new HashMap<>();

        int courseIndex = 0;

        for (Course courseItem : courses) {

            if (courseItem == null || courseItem.getId() == null) {
                continue;
            }

            Course course =
                    courseRepository.findById(courseItem.getId())
                            .orElse(null);

            if (course == null) {
                continue;
            }

            List<Map<String, Object>> studentsStats = new ArrayList<>();

            if (course.getStudents() != null) {

                for (User enrolledStudent : course.getStudents()) {

                    if (enrolledStudent == null || enrolledStudent.getId() == null) {
                        continue;
                    }

                    User student =
                            userRepository.findById(enrolledStudent.getId())
                                    .orElse(null);

                    if (student == null) {
                        continue;
                    }

                    int totalMinutes = getTotalMinutesByStudentAndCourse(student, course);
                    int weeklyMinutes = getWeeklyMinutesByStudentAndCourse(student, course);
                    long sessions = getTotalSessionsByStudentAndCourse(student, course);

                    double totalHours = roundHours(totalMinutes);
                    double weeklyHours = roundHours(weeklyMinutes);
                    double averageDaily = Math.round((weeklyHours / 7.0) * 10.0) / 10.0;

                    Map<String, Object> data = new HashMap<>();

                    data.put("studentName", student.getName());
                    data.put("courseId", course.getId());
                    data.put("courseName", course.getName());
                    data.put("color", getColorByIndex(courseIndex));
                    data.put("totalHours", totalHours);
                    data.put("weeklyHours", weeklyHours);
                    data.put("averageDaily", averageDaily);
                    data.put("sessions", sessions);

                    studentsStats.add(data);
                }
            }

            result.put(course.getId(), studentsStats);

            courseIndex++;
        }

        return result;
    }

    @Transactional
    public int getTotalMinutesByStudentAndCourse(User sessionUser,
                                                 Course course) {

        List<StudySession> sessions = getSessionsByStudent(sessionUser);

        int totalMinutes = 0;

        for (StudySession session : sessions) {

            if (session.getCourse() == null ||
                session.getCourse().getId() == null ||
                course == null ||
                course.getId() == null) {
                continue;
            }

            if (session.getCourse().getId().equals(course.getId())) {
                totalMinutes += session.getMinutes();
            }
        }

        return totalMinutes;
    }

    @Transactional
    public int getWeeklyMinutesByStudentAndCourse(User sessionUser,
                                                  Course course) {

        List<StudySession> sessions = getSessionsByStudent(sessionUser);

        List<StudySession> filtered = new ArrayList<>();

        for (StudySession session : sessions) {

            if (session.getCourse() == null ||
                session.getCourse().getId() == null ||
                course == null ||
                course.getId() == null) {
                continue;
            }

            if (session.getCourse().getId().equals(course.getId())) {
                filtered.add(session);
            }
        }

        return getWeeklyMinutes(filtered);
    }

    @Transactional
    public long getTotalSessionsByStudentAndCourse(User sessionUser,
                                                   Course course) {

        List<StudySession> sessions = getSessionsByStudent(sessionUser);

        long total = 0;

        for (StudySession session : sessions) {

            if (session.getCourse() == null ||
                session.getCourse().getId() == null ||
                course == null ||
                course.getId() == null) {
                continue;
            }

            if (session.getCourse().getId().equals(course.getId())) {
                total++;
            }
        }

        return total;
    }

    private int getWeeklyMinutes(List<StudySession> sessions) {

        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(DayOfWeek.MONDAY);
        LocalDate sunday = today.with(DayOfWeek.SUNDAY);

        int totalMinutes = 0;

        for (StudySession session : sessions) {

            if (session.getFinishedAt() == null) {
                continue;
            }

            LocalDate date = session.getFinishedAt().toLocalDate();

            if (!date.isBefore(monday) && !date.isAfter(sunday)) {
                totalMinutes += session.getMinutes();
            }
        }

        return totalMinutes;
    }

    public double roundHours(int minutes) {
        return Math.round((minutes / 60.0) * 10.0) / 10.0;
    }

    public String getColorByIndex(int index) {

        String[] colors = {
                "#3b82f6",
                "#8b5cf6",
                "#ec4899",
                "#10b981",
                "#f97316",
                "#ef4444",
                "#14b8a6",
                "#6366f1",
                "#f59e0b",
                "#06b6d4"
        };

        return colors[index % colors.length];
    }

    private String getDayKey(DayOfWeek dayOfWeek) {

        return switch (dayOfWeek) {
            case MONDAY -> "lun";
            case TUESDAY -> "mar";
            case WEDNESDAY -> "mié";
            case THURSDAY -> "jue";
            case FRIDAY -> "vie";
            case SATURDAY -> "sáb";
            case SUNDAY -> "dom";
        };
    }
}