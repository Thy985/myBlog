package com.xingchen.backend.controller;

import com.xingchen.backend.common.Result;
import com.xingchen.backend.entity.BlogSetting;
import com.xingchen.backend.mapper.BlogSettingMapper;
import lombok.RequiredArgsConstructor;
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

    @GetMapping("/detail")
    public Result<Map<String, Object>> getDetail() {
        BlogSetting setting = blogSettingMapper.selectOneById(1L);
        
        Map<String, Object> result = new HashMap<>();
        if (setting != null) {
            result.put("name", setting.getBlogName());
            result.put("author", setting.getAuthor());
            result.put("introduction", setting.getIntroduction());
            // 处理头像和logo的完整URL
            String avatar = setting.getAvatar();
            if (avatar != null && !avatar.startsWith("http")) {
                avatar = "/api/file/" + avatar;
            }
            result.put("avatar", avatar);
            String logo = setting.getLogo();
            if (logo != null && !logo.startsWith("http")) {
                logo = "/api/file/" + logo;
            }
            result.put("logo", logo);
            result.put("favicon", setting.getFavicon());
            result.put("github", setting.getGithubHome());
            result.put("gitee", setting.getGiteeHome());
            result.put("csdn", setting.getCsdnHome());
            result.put("zhihu", setting.getZhihuHome());
            result.put("footerInfo", setting.getFooterInfo());
            result.put("beianCode", setting.getBeianCode());
            result.put("seoTitle", setting.getSeoTitle());
            result.put("seoKeywords", setting.getSeoKeywords());
            result.put("seoDescription", setting.getSeoDescription());
        }
        
        return Result.success(result);
    }
}
