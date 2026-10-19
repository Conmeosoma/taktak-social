package com.taktak.server.auth;

import java.util.Properties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

@Component
public class SmtpAccountMail implements AccountMail {
    private final JavaMailSenderImpl sender;
    private final String from;
    private final boolean configured;

    public SmtpAccountMail(@Value("${MAIL_HOST:}") String host, @Value("${MAIL_PORT:587}") int port,
            @Value("${MAIL_USERNAME:}") String username, @Value("${MAIL_PASSWORD:}") String password,
            @Value("${MAIL_FROM:}") String from) {
        sender = new JavaMailSenderImpl(); sender.setHost(host); sender.setPort(port);
        sender.setUsername(username); sender.setPassword(password); sender.setDefaultEncoding("UTF-8");
        Properties properties = sender.getJavaMailProperties();
        properties.setProperty("mail.smtp.auth", "true");
        properties.setProperty("mail.smtp.starttls.enable", "true");
        properties.setProperty("mail.smtp.starttls.required", "true");
        properties.setProperty("mail.smtp.ssl.checkserveridentity", "true");
        properties.setProperty("mail.smtp.connectiontimeout", "5000");
        properties.setProperty("mail.smtp.timeout", "5000");
        properties.setProperty("mail.smtp.writetimeout", "5000");
        properties.setProperty("mail.debug", "false");
        this.from = from;
        configured = !host.isBlank() && !username.isBlank() && !password.isBlank() && !from.isBlank();
    }

    @Override public void send(String recipient, String subject, String text) {
        if (!configured) { throw new org.springframework.mail.MailSendException("Account mail not configured."); }
        var message = new SimpleMailMessage(); message.setFrom(from); message.setTo(recipient);
        message.setSubject(subject); message.setText(text); sender.send(message);
        // SMTP accept không chứng minh thư đã vào inbox; không log recipient/body/credentials.
    }
}
