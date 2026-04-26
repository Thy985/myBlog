package com.xingchen.backend;

import cn.dev33.satoken.secure.BCrypt;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * BCrypt密码加密工具测试
 * 密码从环境变量或测试配置读取，不硬编码在代码中
 */
public class BCryptTest {

    private static final Map<String, String> TEST_PASSWORDS = new HashMap<>();

    static {
        // 从环境变量读取测试密码，默认为测试占位符
        TEST_PASSWORDS.put("admin", System.getenv("TEST_ADMIN_PASSWORD") != null
            ? System.getenv("TEST_ADMIN_PASSWORD") : "TEST_ADMIN_PASSWORD_NOT_SET");
        TEST_PASSWORDS.put("test", System.getenv("TEST_USER_PASSWORD") != null
            ? System.getenv("TEST_USER_PASSWORD") : "TEST_USER_PASSWORD_NOT_SET");
    }

    @Test
    public void testBCrypt() {
        System.out.println("BCrypt密码加密工具");
        System.out.println("=" + "=".repeat(48));

        String adminPassword = TEST_PASSWORDS.get("admin");
        String testPassword = TEST_PASSWORDS.get("test");

        // 只显示密码长度，不显示实际密码
        System.out.println("Admin密码长度: " + adminPassword.length());
        System.out.println("Test密码长度: " + testPassword.length());
        System.out.println();

        // 生成哈希（用于验证工具是否正常工作）
        String adminHash = BCrypt.hashpw(adminPassword);
        String testHash = BCrypt.hashpw(testPassword);
        System.out.println("Admin BCrypt哈希: " + adminHash);
        System.out.println("Test BCrypt哈希: " + testHash);
        System.out.println();

        // 验证密码
        System.out.println("验证测试:");
        System.out.println("Admin密码验证: " + BCrypt.checkpw(adminPassword, adminHash));
        System.out.println("Test密码验证: " + BCrypt.checkpw(testPassword, testHash));
        System.out.println();

        // 验证SQL文件中的密码（使用占位符，实际验证时请替换为真实哈希）
        System.out.println("SQL文件中密码验证:");
        System.out.println("请设置环境变量 TEST_ADMIN_PASSWORD 和 TEST_USER_PASSWORD 运行完整验证");
        System.out.println();

        System.out.println("使用说明:");
        System.out.println("1. 设置环境变量: export TEST_ADMIN_PASSWORD=<your_password>");
        System.out.println("2. 生成新密码哈希: 运行此测试即可看到示例");
        System.out.println("3. 替换SQL文件中的密码: 将生成的哈希值复制到SQL文件中");
        System.out.println("4. 验证密码: 可以使用BCrypt.checkpw方法验证密码是否正确");
    }
}