package com.xingchen.backend;

import cn.dev33.satoken.secure.BCrypt;
import org.junit.jupiter.api.Test;

/**
 * BCrypt密码加密测试
 */
public class BCryptTest {
    
    @Test
    public void testBCrypt() {
        System.out.println("BCrypt密码加密工具");
        System.out.println("=" + "=".repeat(48));
        
        // 测试生成admin用户密码
        String adminPassword = "147258369Thy@";
        String adminHash = BCrypt.hashpw(adminPassword);
        System.out.println("Admin密码: " + adminPassword);
        System.out.println("Admin BCrypt哈希: " + adminHash);
        System.out.println();
        
        // 测试生成test用户密码
        String testPassword = "test1234@";
        String testHash = BCrypt.hashpw(testPassword);
        System.out.println("Test密码: " + testPassword);
        System.out.println("Test BCrypt哈希: " + testHash);
        System.out.println();
        
        // 验证密码
        System.out.println("验证测试:");
        System.out.println("Admin密码验证: " + BCrypt.checkpw(adminPassword, adminHash));
        System.out.println("Test密码验证: " + BCrypt.checkpw(testPassword, testHash));
        System.out.println();
        
        // 验证SQL文件中的密码
        System.out.println("SQL文件中密码验证:");
        String sqlAdminHash = "$2a$10$qwzlMTWMC4loz4fyzdEutuaZyfLpDC6CEBXjLEtjexwjC9MP92RSW";
        String sqlTestHash = "$2a$12$NfOiq8Ynf56nje5mqmbW0eOfeKWBh.t6FMjQ4Ow8VRj2UfBQ8QiIy";
        
        System.out.println("SQL文件中Admin密码验证: " + BCrypt.checkpw(adminPassword, sqlAdminHash));
        System.out.println("SQL文件中Test密码验证: " + BCrypt.checkpw(testPassword, sqlTestHash));
        System.out.println();
        
        System.out.println("使用说明:");
        System.out.println("1. 生成新密码哈希: 运行此测试即可看到示例");
        System.out.println("2. 替换SQL文件中的密码: 将生成的哈希值复制到SQL文件中对应的password字段");
        System.out.println("3. 验证密码: 可以使用BCrypt.checkpw方法验证密码是否正确");
    }
}