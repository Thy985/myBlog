package com.xingchen.backend;

import com.xingchen.backend.dto.LoginDTO;
import com.xingchen.backend.service.UserService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

@SpringBootTest
@Disabled("CI环境无完整数据库，跳过")
public class LoginTest {

    @Autowired
    private UserService userService;

    @Test
    public void testLogin() {
        try {
            LoginDTO dto = new LoginDTO();
            dto.setUsername("admin");
            dto.setPassword("147258369Thy@");
            
            Map<String, Object> result = userService.login(dto, "127.0.0.1", "Test Device");
            System.out.println("Login successful:");
            System.out.println("Token: " + result.get("token"));
            System.out.println("User: " + result.get("user"));
            System.out.println("User ID: " + result.get("userId"));
        } catch (Exception e) {
            System.out.println("Login failed:");
            e.printStackTrace();
        }
    }
}
