package com.xingchen.backend.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.xingchen.backend.aspect.RateLimit;
import com.xingchen.backend.common.Result;
import com.xingchen.backend.dto.MfaCodeDTO;
import com.xingchen.backend.service.MfaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/mfa")
@RequiredArgsConstructor
@Slf4j
@Validated
public class MfaController {

    private final MfaService mfaService;

    @RateLimit(key = "mfa_setup", capacity = 3, timeWindow = 300)
    @PostMapping("/setup")
    public Result<MfaService.MfaSetupResult> setupMfa() {
        Long userId = StpUtil.getLoginIdAsLong();
        String username = StpUtil.getLoginIdAsString();
        MfaService.MfaSetupResult result = mfaService.setupMfa(userId, username);
        log.info("用户 {} 开始设置MFA", userId);
        return Result.success(result);
    }

    @PostMapping("/verify-and-enable")
    public Result<Void> verifyAndEnable(@Valid @RequestBody MfaCodeDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        mfaService.verifyCode(userId, dto.getCode());
        mfaService.enableMfa(userId);
        log.info("用户 {} 成功启用MFA", userId);
        return Result.success();
    }

    @PostMapping("/disable")
    public Result<Void> disableMfa(@Valid @RequestBody MfaCodeDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        mfaService.verifyCode(userId, dto.getCode());
        mfaService.disableMfa(userId);
        log.info("用户 {} 已禁用MFA", userId);
        return Result.success();
    }

    @GetMapping("/status")
    public Result<Map<String, Object>> getMfaStatus() {
        Long userId = StpUtil.getLoginIdAsLong();
        boolean enabled = mfaService.isMfaEnabled(userId);
        return Result.success(Map.of(
                "enabled", enabled,
                "userId", userId
        ));
    }
}
