package com.taktak.server.auth;

public interface AccountMail {
    void send(String recipient, String subject, String text);
}
