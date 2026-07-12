package com.darkcode.spring.app.service;

import com.darkcode.spring.app.model.Submission;
import com.darkcode.spring.app.model.Course;
import com.darkcode.spring.app.model.Task;
import com.darkcode.spring.app.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Service
public class NotificationService {

    @Autowired
    private EmailService emailService;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Notifica a todos los estudiantes del curso
     * cuando el docente publica una tarea.
     */
    public void notificarNuevaTarea(Task task) {

        if (task == null || task.getCourse() == null) {
            return;
        }

        Course course = task.getCourse();

        if (course.getStudents() == null || course.getStudents().isEmpty()) {
            return;
        }

        for (User student : course.getStudents()) {

            if (student.getEmail() == null || student.getEmail().isBlank()) {
                continue;
            }

            String asunto = "📚 Nueva tarea publicada - " + course.getName();

            String contenido =
                    "<h2>¡Hola, " + student.getName() + "!</h2>"
                    + "<p>Tu docente acaba de publicar una nueva tarea.</p>"

                    + "<hr>"

                    + "<p><b>Curso:</b> "
                    + course.getName()
                    + "</p>"

                    + "<p><b>Tarea:</b> "
                    + task.getTitle()
                    + "</p>"

                    + "<p><b>Descripción:</b> "
                    + (task.getDescription() == null
                        ? "Sin descripción."
                        : task.getDescription())
                    + "</p>"

                    + "<p><b>Prioridad:</b> "
                    + task.getPriority()
                    + "</p>"

                    + "<p><b>Fecha límite:</b> "
                    + (task.getDueDate() == null
                        ? "No especificada"
                        : task.getDueDate().format(FORMATTER))
                    + "</p>"

                    + "<hr>"

                    + "<p>Ingresa a <b>Agenda Estudiantil</b> para revisar la actividad.</p>"

                    + "<br>"

                    + "<p>Este correo fue enviado automáticamente.</p>";

            emailService.enviarCorreo(
                    student.getEmail(),
                    asunto,
                    contenido
            );
        }
    }

    /**
     * Notifica a todos los estudiantes cuando
     * el docente publica una práctica calificada.
     */
    public void notificarNuevaPractica(Task task) {

        if (task == null || task.getCourse() == null) {
            return;
        }

        Course course = task.getCourse();

        if (course.getStudents() == null || course.getStudents().isEmpty()) {
            return;
        }

        for (User student : course.getStudents()) {

            if (student.getEmail() == null || student.getEmail().isBlank()) {
                continue;
            }

            String asunto = "📝 Nueva práctica calificada - " + course.getName();

            String contenido =
                    "<h2>¡Hola, " + student.getName() + "!</h2>"
                    + "<p>Tu docente acaba de publicar una nueva práctica calificada.</p>"

                    + "<hr>"

                    + "<p><b>Curso:</b> "
                    + course.getName()
                    + "</p>"

                    + "<p><b>Práctica:</b> "
                    + task.getTitle()
                    + "</p>"

                    + "<p><b>Descripción:</b> "
                    + (task.getDescription() == null
                        ? "Sin descripción."
                        : task.getDescription())
                    + "</p>"

                    + "<p><b>Fecha límite:</b> "
                    + (task.getDueDate() == null
                        ? "No especificada"
                        : task.getDueDate().format(FORMATTER))
                    + "</p>"

                    + "<hr>"

                    + "<p>Ingresa a <b>Agenda Estudiantil</b> para resolver la práctica.</p>"

                    + "<br>"

                    + "<p>Este correo fue enviado automáticamente.</p>";

            emailService.enviarCorreo(
                    student.getEmail(),
                    asunto,
                    contenido
            );
        }
    }
    /**
     * Notifica al docente cuando un estudiante entrega una tarea.
     */
    public void notificarEntrega(Task task, User student) {

        if (task == null || task.getUser() == null || student == null) {
            return;
        }

        User teacher = task.getUser();

        if (teacher.getEmail() == null || teacher.getEmail().isBlank()) {
            return;
        }

        String asunto = "📥 Nueva entrega recibida";

        String contenido =
                "<h2>Hola " + teacher.getName() + "</h2>"

                + "<p>El estudiante <b>"
                + student.getName()
                + "</b> acaba de entregar una actividad.</p>"

                + "<hr>"

                + "<p><b>Curso:</b> "
                + task.getCourse().getName()
                + "</p>"

                + "<p><b>Tarea:</b> "
                + task.getTitle()
                + "</p>"

                + "<hr>"

                + "<p>Ingresa al sistema para revisarla.</p>";

        emailService.enviarCorreo(
                teacher.getEmail(),
                asunto,
                contenido
        );
    }

    /**
     * Notifica al estudiante cuando el docente califica su entrega.
     */
    public void notificarCalificacion(Submission submission) {

        if (submission == null
                || submission.getStudent() == null
                || submission.getTask() == null) {
            return;
        }

        User student = submission.getStudent();

        if (student.getEmail() == null || student.getEmail().isBlank()) {
            return;
        }

        String asunto = "✅ Tu tarea fue calificada";

        String contenido =
                "<h2>Hola " + student.getName() + "</h2>"

                + "<p>Tu docente ya calificó tu actividad.</p>"

                + "<hr>"

                + "<p><b>Tarea:</b> "
                + submission.getTask().getTitle()
                + "</p>"

                + "<p><b>Nota:</b> "
                + submission.getGrade()
                + "</p>"

                + "<hr>"

                + "<p>Ingresa al sistema para verla.</p>";

        emailService.enviarCorreo(
                student.getEmail(),
                asunto,
                contenido
        );
    }

    /**
     * Notifica al estudiante cuando el docente devuelve la tarea.
     */
    public void notificarRevision(Submission submission) {

        if (submission == null
                || submission.getStudent() == null
                || submission.getTask() == null) {
            return;
        }

        User student = submission.getStudent();

        if (student.getEmail() == null || student.getEmail().isBlank()) {
            return;
        }

        String asunto = "📄 Tu entrega fue revisada";

        String contenido =
                "<h2>Hola " + student.getName() + "</h2>"

                + "<p>Tu docente revisó una de tus entregas.</p>"

                + "<hr>"

                + "<p><b>Tarea:</b> "
                + submission.getTask().getTitle()
                + "</p>"

                + "<p>Puedes ingresar al sistema para revisar la retroalimentación.</p>";

        emailService.enviarCorreo(
                student.getEmail(),
                asunto,
                contenido
        );
    }
}