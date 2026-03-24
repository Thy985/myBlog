package com.xingchen.backend.controller;

import com.xingchen.backend.aspect.RateLimit;
import com.xingchen.backend.common.Result;
import com.xingchen.backend.dto.PasswordResetDTO;
import com.xingchen.backend.dto.SendCodeDTO;
import com.xingchen.backend.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/password")
@RequiredArgsConstructor
public class PasswordController {

    private final PasswordResetService passwordResetService;

    @PostMapping("/reset-code")
    @RateLimit(perMinute = 3, message = "发送验证码过于频繁，请稍后再试")
    public Result<Void> sendResetCode(@Valid @RequestBody SendCodeDTO dto) {
        passwordResetService.sendResetCode(dto.getEmail());
        return Result.success();
    }

    @PostMapping("/verify-code")
    public Result<Void> verifyCode(
            @RequestParam String email,
            @RequestParam String code) {
        passwordResetService.verifyCode(email, code);
        return Result.success();
    }

    @PostMapping("/reset")
    public Result<Void> resetPassword(@Valid @RequestBody PasswordResetDTO dto) {
        passwordResetService.resetPassword(dto.getEmail(), dto.getCode(), dto.getNewPassword());
        return Result.success();
    }
}
