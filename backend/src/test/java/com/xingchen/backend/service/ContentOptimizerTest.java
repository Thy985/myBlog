package com.xingchen.backend.service;

import com.xingchen.backend.vo.ArticleListVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ContentOptimizerTest {

    @Autowired
    private ContentOptimizer contentOptimizer;

    @Autowired
    private SEOService seoService;

    @Test
    public void testAnalyzeContent_ShortContent() {
        String shortContent = "这是一篇很短的文章。";

        List<ContentOptimizer.OptimizationIssue> issues =
            contentOptimizer.analyzeContent(shortContent, "测试标题");

        System.out.println("Issues found: " + issues.size());
        for (ContentOptimizer.OptimizationIssue issue : issues) {
            System.out.println("  - " + issue.getType() + ": " + issue.getDescription());
        }

        assertFalse(issues.isEmpty());
        assertTrue(issues.stream().anyMatch(i -> i.getType().equals("CONTENT_TOO_SHORT")));
    }

    @Test
    public void testAnalyzeContent_GoodContent() {
        String goodContent = """
            # Spring Boot 最佳实践

            ## 概述
            Spring Boot 是 Java 开发中最流行的框架之一。

            ## 核心特性
            - 自动配置
            - 嵌入式服务器
            - 生产就绪

            ## 代码示例
            ```java
            @SpringBootApplication
            public class Application {
                public static void main(String[] args) {
                    SpringApplication.run(Application.class, args);
                }
            }
            ```

            ## 总结
            Spring Boot 大大简化了 Spring 应用的开发。
            """;

        List<ContentOptimizer.OptimizationIssue> issues =
            contentOptimizer.analyzeContent(goodContent, "Spring Boot 最佳实践");

        System.out.println("Issues found for good content: " + issues.size());
        for (ContentOptimizer.OptimizationIssue issue : issues) {
            System.out.println("  - " + issue.getType() + " (" + issue.getSeverity() + "): " + issue.getDescription());
        }

        assertTrue(issues.isEmpty() || issues.stream().noneMatch(i -> "HIGH".equals(i.getSeverity())));
    }

    @Test
    public void testSEOAnalysis() {
        String content = """
            # Docker 容器化部署指南

            Docker 是现代软件部署的重要工具。本文介绍如何使用 Docker 进行容器化部署。

            ## 为什么使用 Docker

            Docker 提供了一致的运行环境，从开发到生产环境无缝迁移。

            ## 核心概念
            - 镜像（Image）
            - 容器（Container）
            - 仓库（Repository）

            ## 实战操作

            首先安装 Docker，然后拉取镜像并运行容器。

            ```bash
            docker pull nginx
            docker run -d -p 80:80 nginx
            ```

            ## 总结
            掌握 Docker 是现代开发者必备技能。
            """;

        SEOService.SEOAnalysisResult result = seoService.analyzeSEO(content, "Docker 容器化部署指南", "Docker");

        System.out.println("SEO Analysis:");
        System.out.println("  Keywords: " + result.getKeywords().size());
        for (SEOService.KeywordInfo kw : result.getKeywords().stream().limit(5).toList()) {
            System.out.println("    - " + kw.getKeyword() + " (freq=" + kw.getFrequency() + ", score=" + String.format("%.2f", kw.getScore()) + ")");
        }
        System.out.println("  Optimized Title: " + result.getOptimizedTitle());
        System.out.println("  Meta Description: " + result.getMetaDescription());
        System.out.println("  SEO Score: " + result.getSeoScore().get("totalScore") + " (" + result.getSeoScore().get("grade") + ")");

        assertNotNull(result.getKeywords());
        assertTrue(result.getKeywords().size() > 0);
    }

    @Test
    public void testExtractKeywords() {
        String content = """
            Docker 是现代软件部署的重要工具，Docker 容器化技术发展迅速。
            使用 Docker 可以实现一致的部署环境，Docker 镜像构建简单高效。
            Docker 容器化是云原生应用的基础设施，Docker 技术栈包括 Docker Engine、Docker Compose、Docker Swarm。
            掌握 Docker 技能对于现代开发人员非常重要，Docker 生态系统不断壮大。
            """;

        List<SEOService.KeywordInfo> keywords = seoService.extractKeywords(content, 10);

        System.out.println("Extracted keywords:");
        for (SEOService.KeywordInfo kw : keywords) {
            System.out.println("  - " + kw.getKeyword() + " (freq=" + kw.getFrequency() + ", score=" + String.format("%.2f", kw.getScore()) + ")");
        }

        assertTrue(keywords.size() > 0, "Should extract at least one keyword");
        assertTrue(keywords.stream().anyMatch(k -> k.getKeyword().equalsIgnoreCase("docker")), "Should extract Docker keyword");
    }
}