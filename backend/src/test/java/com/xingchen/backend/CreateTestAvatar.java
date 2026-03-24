package com.xingchen.backend;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Base64;

/**
 * 创建测试头像文件
 */
public class CreateTestAvatar {
    
    // 1x1 像素红色 PNG 图片 (Base64)
    private static final String AVATAR1_BASE64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==";
    
    // 1x1 像素蓝色 PNG 图片 (Base64)
    private static final String AVATAR2_BASE64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";
    
    public static void main(String[] args) throws IOException {
        String uploadDir = "./uploads/images";
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        // 创建头像1（红色）
        createAvatar(uploadDir + "/avatar1.png", AVATAR1_BASE64);
        System.out.println("创建头像1成功: " + uploadDir + "/avatar1.png");
        
        // 创建头像2（蓝色）
        createAvatar(uploadDir + "/avatar2.png", AVATAR2_BASE64);
        System.out.println("创建头像2成功: " + uploadDir + "/avatar2.png");
    }
    
    private static void createAvatar(String path, String base64) throws IOException {
        byte[] data = Base64.getDecoder().decode(base64);
        try (FileOutputStream fos = new FileOutputStream(path)) {
            fos.write(data);
        }
    }
}
