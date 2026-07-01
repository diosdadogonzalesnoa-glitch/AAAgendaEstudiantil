package com.darkcode.spring.app.service;

import com.darkcode.spring.app.model.Course;
import com.darkcode.spring.app.model.Lesson;
import com.darkcode.spring.app.model.LessonGroup;
import com.darkcode.spring.app.model.User;
import com.darkcode.spring.app.repository.CourseRepository;
import com.darkcode.spring.app.repository.LessonRepository;
import com.darkcode.spring.app.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class LessonService {

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    private final Path uploadPath = Paths.get("uploads", "lessons");

    @Transactional
    public void addLesson(Long courseId,
                          User sessionUser,
                          String title,
                          String description,
                          MultipartFile pdfFile) {

        Course course = courseRepository.findById(courseId).orElse(null);
        User teacher = userRepository.findById(sessionUser.getId()).orElse(null);

        if (course == null || teacher == null || pdfFile == null || pdfFile.isEmpty()) {
            return;
        }

        if (course.getUser() == null || !course.getUser().getId().equals(teacher.getId())) {
            return;
        }

        String originalName = pdfFile.getOriginalFilename();

        if (originalName == null || originalName.trim().isEmpty()) {
            originalName = "clase.pdf";
        }

        originalName = Paths.get(originalName).getFileName().toString();

        if (!originalName.toLowerCase().endsWith(".pdf")) {
            throw new RuntimeException("Solo se permiten archivos PDF para publicar clases.");
        }

        String storedName = UUID.randomUUID() + "_" + originalName;

        try {
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Path filePath = uploadPath.resolve(storedName).normalize();

            Files.copy(
                    pdfFile.getInputStream(),
                    filePath,
                    StandardCopyOption.REPLACE_EXISTING
            );

        } catch (Exception e) {
            throw new RuntimeException("Error al subir el PDF: " + e.getMessage());
        }

        Lesson lesson = new Lesson();

        lesson.setTitle(title);
        lesson.setDescription(description);
        lesson.setOriginalFileName(originalName);
        lesson.setStoredFileName(storedName);
        lesson.setLessonDate(LocalDate.now());
        lesson.setCreatedAt(LocalDateTime.now());
        lesson.setCourse(course);
        lesson.setTeacher(teacher);

        lessonRepository.save(lesson);
    }

    @Transactional
    public Lesson getLessonById(Long lessonId) {
        return lessonRepository.findById(lessonId).orElse(null);
    }

    @Transactional
    public List<LessonGroup> getLessonGroupsByCourse(Long courseId) {

        Course course = courseRepository.findById(courseId).orElse(null);

        if (course == null) {
            return new ArrayList<>();
        }

        List<Lesson> lessons =
                lessonRepository.findByCourseOrderByLessonDateAscCreatedAtAsc(course);

        Map<LocalDate, List<Lesson>> grouped = new LinkedHashMap<>();

        for (Lesson lesson : lessons) {
            grouped.computeIfAbsent(lesson.getLessonDate(), k -> new ArrayList<>()).add(lesson);
        }

        List<LessonGroup> result = new ArrayList<>();

        int number = 1;

        for (Map.Entry<LocalDate, List<Lesson>> entry : grouped.entrySet()) {
            result.add(new LessonGroup("Clase " + number, entry.getValue()));
            number++;
        }

        return result;
    }

    @Transactional
    public void deleteLesson(Long lessonId, User sessionUser) {

        Lesson lesson = lessonRepository.findById(lessonId).orElse(null);
        User teacher = userRepository.findById(sessionUser.getId()).orElse(null);

        if (lesson == null || teacher == null) {
            return;
        }

        if (lesson.getTeacher() == null || !lesson.getTeacher().getId().equals(teacher.getId())) {
            return;
        }

        try {
            if (lesson.getStoredFileName() != null) {
                Path filePath = uploadPath.resolve(lesson.getStoredFileName()).normalize();
                Files.deleteIfExists(filePath);
            }
        } catch (Exception ignored) {
        }

        lessonRepository.delete(lesson);
    }

    public Path getUploadPath() {
        return uploadPath;
    }
}