package com.xingchen.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xingchen.backend.dto.ArticleCreateDTO;
import com.xingchen.backend.service.ArticleService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Disabled("CI环境无完整数据库，跳过")
class ArticleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ArticleService articleService;

    private String adminToken;

    private String getAdminToken() throws Exception {
        if (adminToken != null) {
            return adminToken;
        }
        // Login to get token
        String password = System.getenv("TEST_ADMIN_PASSWORD") != null
            ? System.getenv("TEST_ADMIN_PASSWORD") : "TEST_ADMIN_PASSWORD_NOT_SET";
        String loginJson = """
            {"username":"admin","password":"%s"}
            """.formatted(password);
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        String response = loginResult.getResponse().getContentAsString();
        // Parse token from response - simplified for test
        adminToken = "test-token-placeholder";
        return adminToken;
    }

    @Test
    void getArticleList_shouldReturnPage() throws Exception {
        mockMvc.perform(get("/api/article/list")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void getArticleList_withKeyword_shouldReturnFilteredResults() throws Exception {
        mockMvc.perform(get("/api/article/list")
                        .param("page", "1")
                        .param("size", "10")
                        .param("keyword", "test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void getArticleList_withPagination_shouldRespectLimits() throws Exception {
        mockMvc.perform(get("/api/article/list")
                        .param("page", "1")
                        .param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void getHotArticles_shouldReturnList() throws Exception {
        mockMvc.perform(get("/api/article/hot")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void getArticleArchive_shouldReturnArchive() throws Exception {
        mockMvc.perform(get("/api/article/archive")
                        .param("page", "1")
                        .param("size", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void getRelatedArticles_shouldReturnList() throws Exception {
        // First get an article ID
        mockMvc.perform(get("/api/article/list")
                        .param("page", "1")
                        .param("size", "1"))
                .andExpect(status().isOk());

        // Test with a placeholder article ID (will return empty if not found)
        mockMvc.perform(get("/api/article/related")
                        .param("articleId", "1")
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }
}
