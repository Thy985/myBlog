package com.xingchen.backend.service.impl;

import com.xingchen.backend.service.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailServiceImpl implements MailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    @Override
    @Async
    public void sendVerificationCode(String to, String code, String type) {
        log.info("开始发送验证码邮件 - 收件人: {}, 类型: {}", to, type);
        String subject = "XingChen博客 - 验证码";
        String content = buildVerificationCodeEmail(code, type);
        sendHtmlEmail(to, subject, content);
    }

    @Override
    @Async
    public void sendHtmlEmail(String to, String subject, String content) {
        log.info("准备发送邮件 - 发件人: {}, 收件人: {}, 主题: {}", from, to, subject);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);
            
            log.info("正在发送邮件...");
            mailSender.send(message);
            log.info("邮件发送成功: {}", to);
        } catch (MailAuthenticationException e) {
            log.error("邮件认证失败，请检查邮箱配置 - 发件人: {}, 错误: {}", from, e.getMessage());
            log.error("认证失败详情: ", e);
        } catch (MailSendException e) {
            log.error("邮件发送失败 - 收件人: {}, 错误: {}", to, e.getMessage());
            log.error("发送失败详情: ", e);
        } catch (MessagingException e) {
            log.error("邮件消息构建失败: {}, 错误: {}", to, e.getMessage());
            log.error("构建失败详情: ", e);
        } catch (Exception e) {
            log.error("邮件发送未知错误: {}, 错误: {}", to, e.getMessage());
            log.error("未知错误详情: ", e);
        }
    }

    private String buildVerificationCodeEmail(String code, String type) {
        String typeName = "register".equals(type) ? "注册" : "reset".equals(type) ? "重置密码" : "验证";
        
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: 'Microsoft YaHei', Arial, sans-serif; background-color: #f5f5f5; margin: 0; padding: 20px; }
                    .container { max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); overflow: hidden; }
                    .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 30px; text-align: center; }
                    .header h1 { margin: 0; font-size: 24px; }
                    .content { padding: 30px; }
                    .code-box { background-color: #f8f9fa; border: 2px dashed #667eea; border-radius: 8px; padding: 20px; text-align: center; margin: 20px 0; }
                    .code { font-size: 32px; font-weight: bold; color: #667eea; letter-spacing: 8px; }
                    .info { color: #666; font-size: 14px; line-height: 1.8; }
                    .warning { color: #e74c3c; font-size: 12px; margin-top: 20px; padding: 10px; background-color: #fdf2f2; border-radius: 5px; }
                    .footer { text-align: center; padding: 20px; color: #999; font-size: 12px; border-top: 1px solid #eee; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>XingChen 博客</h1>
                    </div>
                    <div class="content">
                        <p class="info">您好！</p>
                        <p class="info">您正在进行<strong>%s</strong>操作，请使用以下验证码完成验证：</p>
                        <div class="code-box">
                            <span class="code">%s</span>
                        </div>
                        <p class="info">验证码有效期为 <strong>15 分钟</strong>，请尽快完成验证。</p>
                        <div class="warning">
                            ⚠️ 如果这不是您本人的操作，请忽略此邮件，您的账号安全不会受到影响。
                        </div>
                    </div>
                    <div class="footer">
                        <p>此邮件由系统自动发送，请勿直接回复。</p>
                        <p>© 2024 XingChen博客 版权所有</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(typeName, code);
    }
}
