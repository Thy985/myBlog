package com.xingchen.backend.service;

public interface VerificationCodeService {
    
    String generateAndSendCode(String email, String type);
    
    void verifyCode(String email, String code, String type);
    
    void invalidateCode(String email, String type);
}
