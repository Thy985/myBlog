package com.xingchen.backend.controller;

import com.xingchen.backend.aspect.RateLimit;
import com.xingchen.backend.common.Result;
import com.xingchen.backend.dto.SendCodeDTO;
import com.xingchen.backend.service.VerificationCodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/verification")
@RequiredArgsConstructor
@Validated
public class VerificationController {

    private final VerificationCodeService verificationCodeService;

    @RateLimit(key = "send_code", capacity = 5, timeWindow = 300, message = "发送过于频繁，请5分钟后再试")
    @PostMapping("/send-code")
    public Result<Void> sendCode(@Valid @RequestBody SendCodeDTO dto) {
        verificationCodeService.generateAndSendCode(dto.getEmail(), dto.getType());
        return Result.success();
    }

    @PostMapping("/verify")
    public Result<Void> verifyCode(@RequestParam String email, 
                                   @RequestParam String code, 
                                   @RequestParam String type) {
        try {
            verificationCodeService.verifyCode(email, code, type);
            return Result.success();
        } catch (Exception e) {
            return Result.error(400, "验证码无效或已过期");
        }
    }
}
