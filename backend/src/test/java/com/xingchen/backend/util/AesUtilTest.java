package com.xingchen.backend.util;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AesUtil 单元测试
 * <p>
 * 测试策略：
 * - AesUtil 使用静态 SECRET_KEY 字段，通过 @PostConstruct 初始化
 * - 单元测试中不使用 Spring 上下文，通过反射手动设置 SECRET_KEY
 * - 使用一个固定的测试密钥来确保测试可重复
 */
@DisplayName("AesUtil AES-256-GCM 加密测试")
class AesUtilTest {

    private static final String TEST_MASTER_KEY = "test-master-key-for-unit-testing-only";

    @BeforeAll
    static void initSecretKey() throws Exception {
        // 通过反射设置 SECRET_KEY，模拟 Spring @PostConstruct 初始化
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] key = Arrays.copyOf(
                md.digest(TEST_MASTER_KEY.getBytes(StandardCharsets.UTF_8)),
                32 // KEY_LENGTH = 256 bits
        );

        Field secretKeyField = AesUtil.class.getDeclaredField("SECRET_KEY");
        secretKeyField.setAccessible(true);
        secretKeyField.set(null, key);

        // 设置 instance 字段，避免 getInstance() 抛异常
        Field instanceField = AesUtil.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, new Object()); // 只需要非 null
    }

    // ==================== 加密测试 ====================

    @Nested
    @DisplayName("encrypt - 加密功能")
    class EncryptTests {

        @Test
        @DisplayName("成功加密明文")
        void encrypt_SuccessfulEncryption() {
            // Arrange
            String plainText = "hello world";

            // Act
            String encrypted = AesUtil.encrypt(plainText);

            // Assert
            assertNotNull(encrypted);
            assertTrue(encrypted.startsWith("ENC:"), "加密结果应以 ENC: 开头");
            assertNotEquals(plainText, encrypted, "密文应与明文不同");
        }

        @Test
        @DisplayName("null 输入直接返回 null")
        void encrypt_NullInputReturnsNull() {
            // Act
            String result = AesUtil.encrypt(null);

            // Assert
            assertNull(result);
        }

        @Test
        @DisplayName("空字符串直接返回空字符串")
        void encrypt_EmptyStringReturnsEmptyString() {
            // Act
            String result = AesUtil.encrypt("");

            // Assert
            assertEquals("", result);
        }

        @Test
        @DisplayName("纯空白字符串直接返回原值")
        void encrypt_BlankStringReturnsAsIs() {
            // Act
            String result = AesUtil.encrypt("   ");

            // Assert
            assertEquals("   ", result);
        }

        @Test
        @DisplayName("已加密的文本不再重复加密")
        void encrypt_AlreadyEncryptedReturnsAsIs() {
            // Arrange
            String alreadyEncrypted = "ENC:c29tZS1kYXRh";

            // Act
            String result = AesUtil.encrypt(alreadyEncrypted);

            // Assert
            assertEquals(alreadyEncrypted, result);
        }

        @Test
        @DisplayName("相同输入产生不同的密文（IV 随机化）")
        void encrypt_SameInputProducesDifferentOutput() {
            // Arrange
            String plainText = "same-input-text";

            // Act
            String encrypted1 = AesUtil.encrypt(plainText);
            String encrypted2 = AesUtil.encrypt(plainText);

            // Assert
            assertNotNull(encrypted1);
            assertNotNull(encrypted2);
            assertNotEquals(encrypted1, encrypted2,
                    "相同明文应产生不同密文（因为 IV 随机）");
        }

        @Test
        @DisplayName("加密中文字符串")
        void encrypt_ChineseCharacters() {
            // Arrange
            String plainText = "这是一段中文测试文本";

            // Act
            String encrypted = AesUtil.encrypt(plainText);

            // Assert
            assertNotNull(encrypted);
            assertTrue(encrypted.startsWith("ENC:"));
        }

        @Test
        @DisplayName("加密特殊字符")
        void encrypt_SpecialCharacters() {
            // Arrange
            String plainText = "!@#$%^&*()_+-=[]{}|;':\",./<>?`~";

            // Act
            String encrypted = AesUtil.encrypt(plainText);

            // Assert
            assertNotNull(encrypted);
            assertTrue(encrypted.startsWith("ENC:"));
        }

        @Test
        @DisplayName("加密 Unicode 表情符号")
        void encrypt_UnicodeEmojis() {
            // Arrange
            String plainText = "Hello 🌍 🎉 🔐";

            // Act
            String encrypted = AesUtil.encrypt(plainText);

            // Assert
            assertNotNull(encrypted);
            assertTrue(encrypted.startsWith("ENC:"));
        }

        @Test
        @DisplayName("加密长文本")
        void encrypt_LongText() {
            // Arrange
            String plainText = "a".repeat(10000);

            // Act
            String encrypted = AesUtil.encrypt(plainText);

            // Assert
            assertNotNull(encrypted);
            assertTrue(encrypted.startsWith("ENC:"));
        }

        @Test
        @DisplayName("加密包含换行符的文本")
        void encrypt_TextWithNewlines() {
            // Arrange
            String plainText = "line1\nline2\r\nline3";

            // Act
            String encrypted = AesUtil.encrypt(plainText);

            // Assert
            assertNotNull(encrypted);
            assertTrue(encrypted.startsWith("ENC:"));
        }
    }

    // ==================== 解密测试 ====================

    @Nested
    @DisplayName("decrypt - 解密功能")
    class DecryptTests {

        @Test
        @DisplayName("成功解密密文")
        void decrypt_SuccessfulDecryption() {
            // Arrange
            String plainText = "secret message";
            String encrypted = AesUtil.encrypt(plainText);

            // Act
            String decrypted = AesUtil.decrypt(encrypted);

            // Assert
            assertEquals(plainText, decrypted);
        }

        @Test
        @DisplayName("加密解密往返测试 - 还原原始文本")
        void encryptDecrypt_RoundtripProducesOriginalText() {
            // Arrange
            String original = "roundtrip-test-data-12345";

            // Act
            String encrypted = AesUtil.encrypt(original);
            String decrypted = AesUtil.decrypt(encrypted);

            // Assert
            assertEquals(original, decrypted);
        }

        @Test
        @DisplayName("null 输入直接返回 null")
        void decrypt_NullInputReturnsNull() {
            // Act
            String result = AesUtil.decrypt(null);

            // Assert
            assertNull(result);
        }

        @Test
        @DisplayName("空字符串直接返回空字符串")
        void decrypt_EmptyStringReturnsEmptyString() {
            // Act
            String result = AesUtil.decrypt("");

            // Assert
            assertEquals("", result);
        }

        @Test
        @DisplayName("非 ENC: 开头的文本直接返回原值")
        void decrypt_NonEncryptedTextReturnsAsIs() {
            // Arrange
            String plainText = "not-encrypted-text";

            // Act
            String result = AesUtil.decrypt(plainText);

            // Assert
            assertEquals(plainText, result);
        }

        @Test
        @DisplayName("无效密文抛出异常")
        void decrypt_InvalidCiphertextThrowsException() {
            // Arrange
            String invalidEncrypted = "ENC:invalid-base64-data!!!";

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> AesUtil.decrypt(invalidEncrypted));
            assertTrue(exception.getMessage().contains("解密失败"),
                    "异常消息应包含'解密失败'");
        }

        @Test
        @DisplayName("使用错误密钥解密抛出异常")
        void decrypt_WrongKeyThrowsException() throws Exception {
            // Arrange - 先用当前密钥加密
            String plainText = "secret-data";
            String encrypted = AesUtil.encrypt(plainText);

            // 修改 SECRET_KEY 为不同的值
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] wrongKey = Arrays.copyOf(
                    md.digest("completely-different-key".getBytes(StandardCharsets.UTF_8)),
                    32
            );

            Field secretKeyField = AesUtil.class.getDeclaredField("SECRET_KEY");
            secretKeyField.setAccessible(true);
            byte[] originalKey = (byte[]) secretKeyField.get(null);

            try {
                secretKeyField.set(null, wrongKey);

                // Act & Assert
                RuntimeException exception = assertThrows(RuntimeException.class,
                        () -> AesUtil.decrypt(encrypted));
                assertTrue(exception.getMessage().contains("解密失败"));
            } finally {
                // 恢复原始密钥
                secretKeyField.set(null, originalKey);
            }
        }

        @Test
        @DisplayName("解密被篡改的密文抛出异常")
        void decrypt_TamperedCiphertextThrowsException() {
            // Arrange
            String plainText = "tamper-test";
            String encrypted = AesUtil.encrypt(plainText);

            // 篡改密文（修改最后一个字符）
            String tampered = encrypted.substring(0, encrypted.length() - 1)
                    + (encrypted.charAt(encrypted.length() - 1) == 'A' ? 'B' : 'A');

            // Act & Assert
            assertThrows(RuntimeException.class,
                    () -> AesUtil.decrypt(tampered));
        }
    }

    // ==================== 往返测试（多种字符集） ====================

    @Nested
    @DisplayName("往返测试 - 加密后解密")
    class RoundtripTests {

        @Test
        @DisplayName("中文字符往返")
        void roundtrip_ChineseCharacters() {
            String original = "你好世界，测试加密解密功能";
            String encrypted = AesUtil.encrypt(original);
            String decrypted = AesUtil.decrypt(encrypted);
            assertEquals(original, decrypted);
        }

        @Test
        @DisplayName("特殊字符往返")
        void roundtrip_SpecialCharacters() {
            String original = "!@#$%^&*()_+-=[]{}|;':\",./<>?`~";
            String encrypted = AesUtil.encrypt(original);
            String decrypted = AesUtil.decrypt(encrypted);
            assertEquals(original, decrypted);
        }

        @Test
        @DisplayName("Unicode 表情往返")
        void roundtrip_UnicodeEmojis() {
            String original = "🎉🌍🔐✨🚀💡🎯🔑";
            String encrypted = AesUtil.encrypt(original);
            String decrypted = AesUtil.decrypt(encrypted);
            assertEquals(original, decrypted);
        }

        @Test
        @DisplayName("混合字符往返")
        void roundtrip_MixedCharacters() {
            String original = "Hello 世界! 🌍 Test 测试 123 @#$";
            String encrypted = AesUtil.encrypt(original);
            String decrypted = AesUtil.decrypt(encrypted);
            assertEquals(original, decrypted);
        }

        @Test
        @DisplayName("JSON 字符串往返")
        void roundtrip_JsonString() {
            String original = "{\"key\":\"value\",\"nested\":{\"arr\":[1,2,3]}}";
            String encrypted = AesUtil.encrypt(original);
            String decrypted = AesUtil.decrypt(encrypted);
            assertEquals(original, decrypted);
        }

        @Test
        @DisplayName("包含 SQL 语句的文本往返")
        void roundtrip_SqlText() {
            String original = "SELECT * FROM users WHERE name = 'O\\'Brien' AND age > 18;";
            String encrypted = AesUtil.encrypt(original);
            String decrypted = AesUtil.decrypt(encrypted);
            assertEquals(original, decrypted);
        }

        @Test
        @DisplayName("API Key 格式文本往返")
        void roundtrip_ApiKeyFormat() {
            String original = "sk-abcdefghijklmnopqrstuvwxyz1234567890";
            String encrypted = AesUtil.encrypt(original);
            String decrypted = AesUtil.decrypt(encrypted);
            assertEquals(original, decrypted);
        }
    }

    // ==================== isEncrypted 测试 ====================

    @Nested
    @DisplayName("isEncrypted - 检查加密状态")
    class IsEncryptedTests {

        @Test
        @DisplayName("已加密文本返回 true")
        void isEncrypted_EncryptedTextReturnsTrue() {
            String encrypted = AesUtil.encrypt("test");
            assertTrue(AesUtil.isEncrypted(encrypted));
        }

        @Test
        @DisplayName("明文返回 false")
        void isEncrypted_PlainTextReturnsFalse() {
            assertFalse(AesUtil.isEncrypted("plain text"));
        }

        @Test
        @DisplayName("null 返回 false")
        void isEncrypted_NullReturnsFalse() {
            assertFalse(AesUtil.isEncrypted(null));
        }

        @Test
        @DisplayName("空字符串返回 false")
        void isEncrypted_EmptyStringReturnsFalse() {
            assertFalse(AesUtil.isEncrypted(""));
        }
    }

    // ==================== maskApiKey 测试 ====================

    @Nested
    @DisplayName("maskApiKey - API Key 掩码")
    class MaskApiKeyTests {

        @Test
        @DisplayName("null 返回 [EMPTY]")
        void maskApiKey_NullReturnsEmpty() {
            assertEquals("[EMPTY]", AesUtil.maskApiKey(null));
        }

        @Test
        @DisplayName("空字符串返回 [EMPTY]")
        void maskApiKey_EmptyReturnsEmpty() {
            assertEquals("[EMPTY]", AesUtil.maskApiKey(""));
        }

        @Test
        @DisplayName("短文本返回 ***")
        void maskApiKey_ShortTextReturnsStars() {
            String plainKey = "sk-abc";
            assertEquals("***", AesUtil.maskApiKey(plainKey));
        }

        @Test
        @DisplayName("长明文显示前3位和后4位掩码")
        void maskApiKey_LongPlainTextShowsMasked() {
            String plainKey = "sk-abcdefghijklmnopqrstuvwxyz";
            String masked = AesUtil.maskApiKey(plainKey);
            assertEquals("sk-***wxyz", masked);
        }

        @Test
        @DisplayName("加密文本先解密再掩码")
        void maskApiKey_EncryptedTextDecryptsThenMasks() {
            String plainKey = "sk-abcdefghijklmnopqrstuvwxyz";
            String encrypted = AesUtil.encrypt(plainKey);
            String masked = AesUtil.maskApiKey(encrypted);
            assertEquals("sk-***wxyz", masked);
        }

        @Test
        @DisplayName("无法解密的加密文本返回 [ENCRYPTED]")
        void maskApiKey_UndecryptableTextReturnsEncrypted() throws Exception {
            // 先用当前密钥加密
            String encrypted = AesUtil.encrypt("secret");

            // 修改密钥使得解密失败
            Field secretKeyField = AesUtil.class.getDeclaredField("SECRET_KEY");
            secretKeyField.setAccessible(true);
            byte[] originalKey = (byte[]) secretKeyField.get(null);

            try {
                secretKeyField.set(null, new byte[32]);
                assertEquals("[ENCRYPTED]", AesUtil.maskApiKey(encrypted));
            } finally {
                secretKeyField.set(null, originalKey);
            }
        }
    }

    // ==================== toLogSafeString 测试 ====================

    @Nested
    @DisplayName("toLogSafeString - 日志安全字符串")
    class ToLogSafeStringTests {

        @Test
        @DisplayName("null 返回 null")
        void toLogSafeString_NullReturnsNull() {
            assertEquals("null", AesUtil.toLogSafeString(null));
        }

        @Test
        @DisplayName("加密文本返回 [ENCRYPTED:掩码]")
        void toLogSafeString_EncryptedText() {
            String plainKey = "sk-abcdefghijklmnopqrstuvwxyz";
            String encrypted = AesUtil.encrypt(plainKey);
            String logSafe = AesUtil.toLogSafeString(encrypted);
            assertTrue(logSafe.startsWith("[ENCRYPTED:"));
            assertTrue(logSafe.endsWith("]"));
            assertFalse(logSafe.contains("abcdefghijklmnopqrstuvwxyz"));
        }

        @Test
        @DisplayName("明文返回 [PLAIN:掩码]")
        void toLogSafeString_PlainText() {
            String plainKey = "sk-abcdefghijklmnopqrstuvwxyz";
            String logSafe = AesUtil.toLogSafeString(plainKey);
            assertTrue(logSafe.startsWith("[PLAIN:"));
            assertTrue(logSafe.endsWith("]"));
            assertFalse(logSafe.contains("abcdefghijklmnopqrstuvwxyz"));
        }
    }
}
