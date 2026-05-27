package com.xingchen.backend.meta.lightweight;

import com.xingchen.backend.cache.CacheService;
import com.xingchen.backend.prompt.PromptTemplateManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 微型元能力层单元测试
 */
@ExtendWith(MockitoExtension.class)
class MetaLayerTest {

    @Mock
    private CacheService cacheService;

    private IntentTemplateMatcher intentMatcher;
    private UserPreferenceLearner preferenceLearner;
    private PromptTemplateManager templateManager;

    @BeforeEach
    void setUp() {
        intentMatcher = new IntentTemplateMatcher();
        preferenceLearner = new UserPreferenceLearner(cacheService);
        templateManager = new PromptTemplateManager();
    }

    @Nested
    @DisplayName("IntentTemplateMatcher 意图匹配测试")
    class IntentTemplateMatcherTest {

        @Test
        @DisplayName("应正确匹配代码相关输入")
        void shouldMatchCodeRelatedInput() {
            var result = intentMatcher.match("帮我写一个Spring Boot的REST接口");

            assertEquals("code", result.getTemplateKey());
            assertTrue(result.getConfidence() >= 0.3);
        }

        @Test
        @DisplayName("应正确匹配博客文章输入")
        void shouldMatchBlogRelatedInput() {
            var result = intentMatcher.match("帮我写一篇关于Vue3的技术博客");

            assertEquals("blog", result.getTemplateKey());
            assertTrue(result.getConfidence() >= 0.3);
        }

        @Test
        @DisplayName("应正确匹配代码审查输入")
        void shouldMatchReviewRelatedInput() {
            var result = intentMatcher.match("帮我审查这段代码");

            assertEquals("review", result.getTemplateKey());
            assertTrue(result.getConfidence() >= 0.3);
        }

        @Test
        @DisplayName("应正确匹配解释类输入")
        void shouldMatchExplainRelatedInput() {
            var result = intentMatcher.match("什么是依赖注入");

            assertEquals("explain", result.getTemplateKey());
            assertTrue(result.getConfidence() >= 0.3);
        }

        @Test
        @DisplayName("默认输入应返回 chat 模板")
        void shouldReturnChatTemplateForDefaultInput() {
            var result = intentMatcher.match("今天天气怎么样");

            assertEquals("chat", result.getTemplateKey());
            assertTrue(result.getConfidence() >= 0.3);
        }

        @Test
        @DisplayName("应支持多关键词组合匹配")
        void shouldMatchMultipleKeywords() {
            var result = intentMatcher.match("帮我写一个关于Spring Boot的代码");

            assertEquals("code", result.getTemplateKey());
            assertTrue(result.getConfidence() > 0.5);
        }

        @Test
        @DisplayName("应支持新模板 code_review")
        void shouldMatchCodeReview() {
            var result = intentMatcher.match("帮我审查这段代码");

            assertEquals("code_review", result.getTemplateKey());
        }

        @Test
        @DisplayName("应支持新模板 creative")
        void shouldMatchCreative() {
            var result = intentMatcher.match("帮我写一个营销文案");

            assertEquals("creative", result.getTemplateKey());
        }
    }

    @Nested
    @DisplayName("UserPreferenceLearner 偏好学习测试")
    class UserPreferenceLearnerTest {

        @Test
        @DisplayName("新用户应返回默认个性化设置")
        void shouldReturnDefaultPersonalizationForNewUser() {
            when(cacheService.get(anyString(), eq(UserPreferenceLearner.UserPreference.class)))
                    .thenReturn(Optional.empty());

            var personalization = preferenceLearner.getPersonalization(1L, "code");

            assertEquals("neutral", personalization.getTone());
            assertEquals("normal", personalization.getDetailLevel());
            assertNotNull(personalization.getPreferredTemplates());
        }

        @Test
        @DisplayName("应正确学习用户偏好")
        void shouldLearnUserPreference() {
            Long userId = 1L;
            String templateKey = "code";
            boolean positive = true;

            preferenceLearner.learn(userId, templateKey, positive);

            verify(cacheService).set(
                    eq("user:pref:" + userId),
                    any(UserPreferenceLearner.UserPreference.class),
                    any()
            );
        }

        @Test
        @DisplayName("应正确应用个性化")
        void shouldApplyPersonalization() {
            var personalization = UserPreferenceLearner.Personalization.builder()
                    .tone("technical")
                    .detailLevel("detailed")
                    .preferredTemplates(java.util.List.of("code"))
                    .build();

            String prompt = "帮我写代码";
            String result = preferenceLearner.applyPersonalization(prompt, personalization);

            assertNotNull(result);
            assertTrue(result.contains("技术术语") || result.contains(prompt));
        }
    }

    @Nested
    @DisplayName("PromptTemplateManager 模板管理测试")
    class PromptTemplateManagerTest {

        @Test
        @DisplayName("应正确获取默认模板")
        void shouldGetDefaultTemplate() {
            String template = templateManager.getTemplate("default");

            assertNotNull(template);
            assertTrue(template.contains("智能助手"));
        }

        @Test
        @DisplayName("应正确获取代码模板")
        void shouldGetCodeTemplate() {
            String template = templateManager.getTemplate("code");

            assertNotNull(template);
            assertTrue(template.contains("编程助手"));
        }

        @Test
        @DisplayName("应正确获取博客模板")
        void shouldGetBlogTemplate() {
            String template = templateManager.getTemplate("blog");

            assertNotNull(template);
            assertTrue(template.contains("技术博客"));
        }

        @Test
        @DisplayName("应返回默认模板当 key 不存在时")
        void shouldReturnDefaultWhenKeyNotExists() {
            String template = templateManager.getTemplate("non_existent_key");

            assertNotNull(template);
            assertEquals(templateManager.getTemplate("default"), template);
        }

        @Test
        @DisplayName("应正确渲染模板变量")
        void shouldRenderTemplateVariables() {
            var variables = java.util.Map.of(
                    "knowledge_context", "这是测试知识",
                    "memory_context", "用户偏好"
            );

            String result = templateManager.render("rag", variables);

            assertTrue(result.contains("这是测试知识"));
        }

        @Test
        @DisplayName("应正确列出所有模板")
        void shouldListAllTemplates() {
            var templates = templateManager.listTemplates();

            assertNotNull(templates);
            assertFalse(templates.isEmpty());
            assertTrue(templates.containsKey("default"));
            assertTrue(templates.containsKey("code"));
            assertTrue(templates.containsKey("blog"));
        }

        @Test
        @DisplayName("应正确注册自定义模板")
        void shouldRegisterCustomTemplate() {
            String customTemplate = "这是一个自定义模板";
            templateManager.registerTemplate("custom", customTemplate);

            String result = templateManager.getTemplate("custom");

            assertEquals(customTemplate, result);
        }
    }
}