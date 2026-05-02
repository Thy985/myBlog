package com.xingchen.backend.controller;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import com.xingchen.backend.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

/**
 * 验证码控制器
 * 提供图片验证码接口，用于登录防刷
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CaptchaController {

    private static final String CAPTCHA_CODE_KEY = "CAPTCHA_CODE";
    private static final int WIDTH = 120;
    private static final int HEIGHT = 40;
    private static final int CODE_LENGTH = 4;

    /**
     * 获取图片验证码
     * 生成验证码图片并返回，同时将验证码存入 session
     * 返回原生 PNG 图片流
     */
    @GetMapping(value = "/captcha", produces = "image/png")
    public void getCaptcha(HttpServletRequest request, HttpSession session, OutputStream outputStream) throws IOException {
        // 创建图片验证码，使用线条干扰
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(WIDTH, HEIGHT, CODE_LENGTH, 50);
        captcha.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 24));

        String code = captcha.getCode();
        session = request.getSession(true);
        session.setAttribute(CAPTCHA_CODE_KEY, code);

        // 直接写入 PNG 图片流
        BufferedImage image = captcha.getImage();
        ImageIO.write(image, "PNG", outputStream);
        outputStream.flush();
    }

    /**
     * 获取 base64 格式的验证码图片
     * 返回 JSON 格式，包含 base64 数据和 captchaId
     */
    @GetMapping("/captcha/base64")
    public Result<Map<String, String>> getCaptchaBase64(HttpServletRequest request) {
        // 创建图片验证码，使用线条干扰
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(WIDTH, HEIGHT, CODE_LENGTH, 50);
        captcha.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 24));

        String code = captcha.getCode();
        String captchaId = UUID.randomUUID().toString();
        HttpSession session = request.getSession(true);
        session.setAttribute(CAPTCHA_CODE_KEY, code);

        Map<String, String> data = new HashMap<>();
        data.put("image", "data:image/png;base64," + captcha.getImageBase64());
        data.put("captchaId", captchaId);

        return Result.success(data);
    }

    /**
     * 验证验证码（可选接口，供前端校验）
     */
    @GetMapping("/captcha/verify")
    public Map<String, Object> verifyCaptcha(HttpServletRequest request, String code) {
        HttpSession session = request.getSession(false);
        String cachedCode = session != null ? (String) session.getAttribute(CAPTCHA_CODE_KEY) : null;

        Map<String, Object> result = new HashMap<>();
        if (cachedCode != null && cachedCode.equalsIgnoreCase(code)) {
            session.removeAttribute(CAPTCHA_CODE_KEY); // 验证成功后删除
            result.put("code", 200);
            result.put("data", true);
        } else {
            result.put("code", 400);
            result.put("message", "验证码错误");
            result.put("data", false);
        }
        return result;
    }
}
