package com.darkcode.spring.app.service;

import com.sendgrid.SendGrid;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;

import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class EmailService {

    @Autowired
    private SendGrid sendGrid;

    @Value("${sendgrid.from-email}")
    private String fromEmail;

    @Value("${sendgrid.from-name}")
    private String fromName;

    public void enviarCorreo(String destinatario,
                             String asunto,
                             String contenido) {

        Email from = new Email(fromEmail, fromName);
        Email to = new Email(destinatario);

        Content content =
                new Content("text/html", contenido);

        Mail mail =
                new Mail(from, asunto, to, content);

        Request request = new Request();

        try {

            request.setMethod(Method.POST);

            request.setEndpoint("mail/send");

            request.setBody(mail.build());

            Response response =
                    sendGrid.api(request);

            System.out.println(
                    "Correo enviado. Código: "
                            + response.getStatusCode()
            );

        } catch (IOException e) {

            e.printStackTrace();

        }
    }
}