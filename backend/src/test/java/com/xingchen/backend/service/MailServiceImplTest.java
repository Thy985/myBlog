package com.xingchen.backend.service;

import com.xingchen.backend.service.impl.MailServiceImpl;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MailServiceImpl Tests")
class MailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    private MailServiceImpl mailService;

    @BeforeEach
    void setUp() {
        mailService = new MailServiceImpl(mailSender);
        ReflectionTestUtils.setField(mailService, "from", "noreply@xingchen.com");
    }

    @Nested
    @DisplayName("sendHtmlEmail()")
    class SendHtmlEmailTests {

        @Test
        @DisplayName("should send email successfully")
        void sendHtmlEmail_success() throws MessagingException {
            MimeMessage mimeMessage = mock(MimeMessage.class);
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            assertDoesNotThrow(() ->
                    mailService.sendHtmlEmail("test@example.com", "Test Subject", "<h1>Test Content</h1>"));

            verify(mailSender).createMimeMessage();
            verify(mailSender).send(mimeMessage);
        }

        @Test
        @DisplayName("should handle MailAuthenticationException")
        void sendHtmlEmail_authenticationFailure() throws MessagingException {
            MimeMessage mimeMessage = mock(MimeMessage.class);
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
            doThrow(new MailAuthenticationException("Authentication failed"))
                    .when(mailSender).send(any(MimeMessage.class));

            assertDoesNotThrow(() ->
                    mailService.sendHtmlEmail("test@example.com", "Test Subject", "<h1>Test</h1>"));
        }

        @Test
        @DisplayName("should handle MailSendException")
        void sendHtmlEmail_sendFailure() throws MessagingException {
            MimeMessage mimeMessage = mock(MimeMessage.class);
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
            doThrow(new MailSendException("Send failed"))
                    .when(mailSender).send(any(MimeMessage.class));

            assertDoesNotThrow(() ->
                    mailService.sendHtmlEmail("test@example.com", "Test Subject", "<h1>Test</h1>"));
        }

        @Test
        @DisplayName("should handle generic exception")
        void sendHtmlEmail_genericException() throws MessagingException {
            MimeMessage mimeMessage = mock(MimeMessage.class);
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
            doThrow(new RuntimeException("Unexpected error"))
                    .when(mailSender).send(any(MimeMessage.class));

            assertDoesNotThrow(() ->
                    mailService.sendHtmlEmail("test@example.com", "Test Subject", "<h1>Test</h1>"));
        }
    }

    @Nested
    @DisplayName("sendVerificationCode()")
    class SendVerificationCodeTests {

        @Test
        @DisplayName("should call sendHtmlEmail for register type")
        void sendVerificationCode_registerType_callsCorrectly() throws MessagingException {
            MimeMessage mimeMessage = mock(MimeMessage.class);
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            assertDoesNotThrow(() ->
                    mailService.sendVerificationCode("test@example.com", "123456", "register"));

            verify(mailSender).createMimeMessage();
            verify(mailSender).send(mimeMessage);
        }

        @Test
        @DisplayName("should call sendHtmlEmail for reset type")
        void sendVerificationCode_resetType_callsCorrectly() throws MessagingException {
            MimeMessage mimeMessage = mock(MimeMessage.class);
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            assertDoesNotThrow(() ->
                    mailService.sendVerificationCode("test@example.com", "654321", "reset"));

            verify(mailSender).createMimeMessage();
            verify(mailSender).send(mimeMessage);
        }

        @Test
        @DisplayName("should handle unknown type")
        void sendVerificationCode_unknownType_callsCorrectly() throws MessagingException {
            MimeMessage mimeMessage = mock(MimeMessage.class);
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            assertDoesNotThrow(() ->
                    mailService.sendVerificationCode("test@example.com", "111222", "unknown"));

            verify(mailSender).createMimeMessage();
            verify(mailSender).send(mimeMessage);
        }
    }
}
