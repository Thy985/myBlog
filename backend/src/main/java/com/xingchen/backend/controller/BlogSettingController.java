package com.xingchen.backend.controller;

import com.mybatisflex.core.query.QueryWrapper;
import com.xingchen.backend.common.Result;
import com.xingchen.backend.config.MinioConfig;
import com.xingchen.backend.entity.BlogSetting;
import com.xingchen.backend.mapper.BlogSettingMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/blog/setting")
@RequiredArgsConstructor
public class BlogSettingController {

    private final BlogSettingMapper blogSettingMapper;
    private final MinioConfig minioConfig;

    @GetMapping("/detail")
    public Result<Map<String, Object>> getDetail() {
        BlogSetting setting = blogSettingMapper.selectOneByQuery(
            QueryWrapper.create().from(BlogSetting.class).orderBy("id").limit(1)
        );

        Map<String, Object> result = new HashMap<>();
        if (setting != null) {
            result.put("blogName", setting.getBlogName());
            result.put("author", setting.getAuthor());
            result.put("description", setting.getIntroduction());
            result.put("introduction", setting.getIntroduction());

            // 处理头像URL - 返回完整可访问的URL
            String avatar = setting.getAvatar();
            if (avatar != null && !avatar.isEmpty()) {
                avatar = avatar.trim();
                // 如果是完整URL，直接使用
                if (avatar.startsWith("http://") || avatar.startsWith("https://")) {
                    // 保持原样
                } else {
                    // 移除可能的前导斜杠
                    if (avatar.startsWith("/")) {
                        avatar = avatar.substring(1);
                    }
                    // 移除可能存在的 /api/file/ 前缀
                    if (avatar.startsWith("api/file/")) {
                        avatar = avatar.substring("api/".length());
                    }
                    // 文件实际在本地 /uploads/images/ 目录下
                    // 返回完整的 URL
                    avatar = "http://localhost:8080/uploads/images/" + avatar;
                }
            }
            result.put("avatar", avatar);

            // 处理Logo URL - 同样返回完整可访问的URL
            String logo = setting.getLogo();
            if (logo != null && !logo.isEmpty()) {
                logo = logo.trim();
                if (logo.startsWith("http://") || logo.startsWith("https://")) {
                    // 保持原样
                } else {
                    if (logo.startsWith("/")) {
                        logo = logo.substring(1);
                    }
                    if (logo.startsWith("api/file/")) {
                        logo = logo.substring("api/".length());
                    }
                    logo = "http://localhost:8080/uploads/images/" + logo;
                }
            }
            result.put("logo", logo);

            result.put("favicon", setting.getFavicon());
            result.put("github", setting.getGithubHome());
            result.put("gitee", setting.getGiteeHome());
            result.put("csdn", setting.getCsdnHome());
            result.put("zhihu", setting.getZhihuHome());
            result.put("email", setting.getEmail());
            result.put("footerInfo", setting.getFooterInfo());
            result.put("beianCode", setting.getBeianCode());
            result.put("beianLink", setting.getBeianLink());
            result.put("seoTitle", setting.getSeoTitle());
            result.put("seoKeywords", setting.getSeoKeywords());
            result.put("seoDescription", setting.getSeoDescription());
            result.put("siteUrl", "/");
        }

        return Result.success(result);
    }
}
