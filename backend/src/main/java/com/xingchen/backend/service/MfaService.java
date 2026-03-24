package com.xingchen.backend.service;

import lombok.Data;

public interface MfaService {
    @Data
    class MfaSetupResult {
        private String secret;
        private String qrCodeUrl;
        private String manualEntryKey;
    }

    MfaSetupResult setupMfa(Long userId, String username);

    void verifyCode(Long userId, String code);

    void enableMfa(Long userId);

    void disableMfa(Long userId);

    boolean isMfaEnabled(Long userId);
}
