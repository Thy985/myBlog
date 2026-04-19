package com.xingchen.backend.service;

import java.util.Map;

/**
 * 图片服务接口
 * 提供文章封面图获取能力
 */
public interface ImageService {

    /**
     * 根据关键词搜索免费图片
     *
     * @param keyword 关键词
     * @param width 期望宽度
     * @param height 期望高度
     * @return 图片URL
     */
    String searchCoverImage(String keyword, int width, int height);

    /**
     * 获取随机图片
     *
     * @param width 宽度
     * @param height 高度
     * @return 图片URL
     */
    String getRandomImage(int width, int height);

    /**
     * 生成文章封面图提示词
     *
     * @param title 文章标题
     * @param topic 文章主题
     * @return 封面图提示词
     */
    String generateCoverPrompt(String title, String topic);

    /**
     * 检查服务是否可用
     */
    boolean isAvailable();
}
