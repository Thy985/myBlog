package com.xingchen.backend;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

/**
 * 更新用户头像路径
 */
public class UpdateUserAvatar {
    
    public static void main(String[] args) {
        // 数据库配置（从 application.yaml 获取）
        String url = "jdbc:mysql://localhost:3306/myblog?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true";
        String username = "root";
        String password = "123456";
        
        // 头像路径
        String avatar1 = "/uploads/images/avatar1.png";
        String avatar2 = "/uploads/images/avatar2.png";
        
        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            // 更新用户1的头像
            String sql1 = "UPDATE t_user SET avatar = ? WHERE id = 1";
            try (PreparedStatement pstmt = conn.prepareStatement(sql1)) {
                pstmt.setString(1, avatar1);
                int rows = pstmt.executeUpdate();
                System.out.println("用户1头像更新成功: " + avatar1 + " (影响行数: " + rows + ")");
            }
            
            // 更新用户2的头像
            String sql2 = "UPDATE t_user SET avatar = ? WHERE id = 2";
            try (PreparedStatement pstmt = conn.prepareStatement(sql2)) {
                pstmt.setString(1, avatar2);
                int rows = pstmt.executeUpdate();
                System.out.println("用户2头像更新成功: " + avatar2 + " (影响行数: " + rows + ")");
            }
            
            System.out.println("\n数据库更新完成！");
            System.out.println("用户1头像: " + avatar1);
            System.out.println("用户2头像: " + avatar2);
            System.out.println("\n访问地址:");
            System.out.println("http://localhost:8080" + avatar1);
            System.out.println("http://localhost:8080" + avatar2);
            
        } catch (Exception e) {
            System.err.println("数据库更新失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
