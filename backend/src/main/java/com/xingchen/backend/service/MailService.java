package com.xingchen.backend.service;

public interface MailService {
    
    void sendVerificationCode(String to, String code, String type);
    
    void sendHtmlEmail(String to, String subject, String content);
}
