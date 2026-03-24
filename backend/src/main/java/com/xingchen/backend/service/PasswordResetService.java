package com.xingchen.backend.service;

public interface PasswordResetService {
    void sendResetCode(String email);

    void verifyCode(String email, String code);

    void resetPassword(String email, String code, String newPassword);
    
    String getResetToken(String email, String code);
    
    void resetPasswordByToken(String token, String newPassword);
}
