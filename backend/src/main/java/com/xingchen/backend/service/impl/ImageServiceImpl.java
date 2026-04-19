package com.xingchen.backend.service.impl;

import com.xingchen.backend.service.ImageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Random;

/**
 * 图片服务实现
 * 支持 Unsplash、Lorem Picsum 等免费图片源
 */
@Service
@Slf4j
public class ImageServiceImpl implements ImageService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final Random random = new Random();

    @Value("${image.unsplash.access-key:}")
    private String unsplashAccessKey;

    @Value("${image.service.enabled:true}")
    private boolean serviceEnabled;

    private static final String[] PICSUM_BASE_URLS = {
            "https://picsum.photos",
            "https://loremflickr.com",
            "https://placeholder.com"
    };

    @Override
    public String searchCoverImage(String keyword, int width, int height) {
        if (!serviceEnabled) {
            return getDefaultImage(width, height);
        }

        try {
            // 优先尝试使用 Unsplash API
            if (unsplashAccessKey != null && !unsplashAccessKey.isEmpty()) {
                return searchUnsplash(keyword, width, height);
            }

            // 使用 Lorem Picsum（基于关键词生成种子）
            return getPicsumWithKeyword(keyword, width, height);

        } catch (Exception e) {
            log.warn("图片搜索失败，使用默认图片: {}", e.getMessage());
            return getDefaultImage(width, height);
        }
    }

    @Override
    public String getRandomImage(int width, int height) {
        if (!serviceEnabled) {
            return getDefaultImage(width, height);
        }

        try {
            // 使用 Picsum 随机图片
            int seed = random.nextInt(1000);
            return String.format("https://picsum.photos/seed/%d/%d/%d", seed, width, height);
        } catch (Exception e) {
            log.warn("获取随机图片失败: {}", e.getMessage());
            return getDefaultImage(width, height);
        }
    }

    @Override
    public String generateCoverPrompt(String title, String topic) {
        // 根据文章标题和主题生成封面图提示词
        StringBuilder prompt = new StringBuilder();
        prompt.append("Cover image for article about ").append(topic != null ? topic : title);

        // 添加风格描述
        prompt.append(", modern flat design, clean background, ");
        prompt.append("no text, high quality, ");
        prompt.append("suitable for blog cover");

        return prompt.toString();
    }

    @Override
    public boolean isAvailable() {
        return serviceEnabled;
    }

    /**
     * 使用 Unsplash API 搜索图片
     */
    private String searchUnsplash(String keyword, int width, int height) {
        try {
            String url = String.format(
                    "https://api.unsplash.com/photos/random?query=%s&w=%d&h=%d",
                    keyword.replace(" ", "+"),
                    width,
                    height
            );

            // 注意：实际使用时需要添加 Authorization header
            // 这里返回构建好的URL，实际项目中可以通过配置access-key来调用API
            return String.format(
                    "https://source.unsplash.com/%dx%d/?%s",
                    width,
                    height,
                    keyword.replace(" ", "+")
            );
        } catch (Exception e) {
            log.warn("Unsplash 搜索失败: {}", e.getMessage());
            return getPicsumWithKeyword(keyword, width, height);
        }
    }

    /**
     * 使用 Picsum 根据关键词生成图片
     */
    private String getPicsumWithKeyword(String keyword, int width, int height) {
        // 将关键词转换为种子数字
        int seed = keyword.hashCode() & 0x7FFFFFFF;
        return String.format("https://picsum.photos/seed/%d/%d/%d", seed, width, height);
    }

    /**
     * 获取默认图片
     */
    private String getDefaultImage(int width, int height) {
        // 返回一个渐变占位图
        return String.format("https://via.placeholder.com/%dx%d/4A90E2/FFFFFF?text=Article+Cover", width, height);
    }

    /**
     * 从多个源获取备选图片
     */
    public String getFallbackImage(String topic, int width, int height) {
        // 尝试多个免费图片源
        String[] urls = {
                // Picsum 随机
                String.format("https://picsum.photos/%d/%d", width, height),
                // Lorem Flickr 基于主题
                String.format("https://loremflickr.com/%d/%d/%s", width, height, topic != null ? topic.replace(" ", "_") : "technology"),
                // 渐变占位图
                String.format("https://via.placeholder.com/%dx%d/667EEa/FFFFFF?text=%s",
                        width, height, (topic != null ? topic.substring(0, Math.min(10, topic.length())) : "Article").replace(" ", "+"))
        };

        return urls[random.nextInt(urls.length)];
    }
}
