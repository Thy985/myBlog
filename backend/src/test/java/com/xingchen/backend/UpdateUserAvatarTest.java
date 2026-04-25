package com.xingchen.backend;

import com.mybatisflex.core.query.QueryWrapper;
import com.xingchen.backend.entity.User;
import com.xingchen.backend.mapper.UserMapper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 更新用户头像测试
 */
@SpringBootTest
@Disabled("CI环境无完整数据库，跳过")
public class UpdateUserAvatarTest {

    @Autowired
    private UserMapper userMapper;

    @Test
    public void updateUserAvatars() {
        // 头像路径
        String avatar1 = "/uploads/images/avatar1.png";
        String avatar2 = "/uploads/images/avatar2.png";

        // 更新用户1的头像
        User user1 = userMapper.selectOneById(1L);
        if (user1 != null) {
            user1.setAvatar(avatar1);
            userMapper.update(user1);
            System.out.println("用户1头像更新成功: " + avatar1);
        } else {
            System.out.println("用户1不存在");
        }

        // 更新用户2的头像
        User user2 = userMapper.selectOneById(2L);
        if (user2 != null) {
            user2.setAvatar(avatar2);
            userMapper.update(user2);
            System.out.println("用户2头像更新成功: " + avatar2);
        } else {
            System.out.println("用户2不存在");
        }

        System.out.println("\n数据库更新完成！");
        System.out.println("用户1头像: " + avatar1);
        System.out.println("用户2头像: " + avatar2);
        System.out.println("\n访问地址:");
        System.out.println("http://localhost:8080" + avatar1);
        System.out.println("http://localhost:8080" + avatar2);
    }
}
