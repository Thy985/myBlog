package com.xingchen.backend.service;

import com.xingchen.backend.messaging.EmbeddingProducer;
import com.xingchen.backend.repository.ArticleSearchRepository.HybridSearchResult;
import com.xingchen.backend.service.impl.KnowledgeBaseServiceImplV2;
import com.xingchen.backend.vector.HybridSearchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("KnowledgeBaseServiceImplV2 Tests")
class KnowledgeBaseServiceImplV2Test {

    @Mock
    private HybridSearchService hybridSearchService;

    @Mock
    private EmbeddingProducer embeddingProducer;

    @InjectMocks
    private KnowledgeBaseServiceImplV2 knowledgeBaseService;

    @Nested
    @DisplayName("addDocument()")
    class AddDocumentTests {

        @Test
        @DisplayName("should send index task with default category")
        void addDocument_defaultCategory_sendsIndexTask() {
            doNothing().when(embeddingProducer).sendIndexTask(anyLong(), anyString(), anyString(), anyString());

            knowledgeBaseService.addDocument(1L, "Test Title", "Test Content");

            verify(embeddingProducer).sendIndexTask(1L, "Test Title", "Test Content", "default");
        }

        @Test
        @DisplayName("should send index task with specified category")
        void addDocument_specifiedCategory_sendsIndexTask() {
            doNothing().when(embeddingProducer).sendIndexTask(anyLong(), anyString(), anyString(), anyString());

            knowledgeBaseService.addDocument(1L, "Test Title", "Test Content", "tech");

            verify(embeddingProducer).sendIndexTask(1L, "Test Title", "Test Content", "tech");
        }
    }

    @Nested
    @DisplayName("deleteDocument()")
    class DeleteDocumentTests {

        @Test
        @DisplayName("should send delete task")
        void deleteDocument_sendsDeleteTask() {
            doNothing().when(embeddingProducer).sendDeleteTask(anyLong());

            knowledgeBaseService.deleteDocument(1L);

            verify(embeddingProducer).sendDeleteTask(1L);
        }
    }

    @Nested
    @DisplayName("search()")
    class SearchTests {

        @Test
        @DisplayName("should return formatted search results")
        void search_withResults_returnsFormattedString() {
            HybridSearchResult result1 = mock(HybridSearchResult.class);
            HybridSearchResult result2 = mock(HybridSearchResult.class);

            when(result1.title()).thenReturn("Article 1");
            when(result1.getRrfScore()).thenReturn(0.85);
            when(result1.getSummary()).thenReturn("Summary of article 1");

            when(result2.title()).thenReturn("Article 2");
            when(result2.getRrfScore()).thenReturn(0.72);
            when(result2.getSummary()).thenReturn("Summary of article 2");

            when(hybridSearchService.hybridSearch("test query", 5))
                    .thenReturn(Arrays.asList(result1, result2));

            String result = knowledgeBaseService.search("test query");

            assertNotNull(result);
            assertTrue(result.contains("Article 1"));
            assertTrue(result.contains("Article 2"));
            assertTrue(result.contains("相关度"));
            assertTrue(result.contains("0.85"));
            assertTrue(result.contains("0.72"));
        }

        @Test
        @DisplayName("should return empty string when no results")
        void search_noResults_returnsEmptyString() {
            when(hybridSearchService.hybridSearch("test query", 5))
                    .thenReturn(Collections.emptyList());

            String result = knowledgeBaseService.search("test query");

            assertEquals("", result);
        }

        @Test
        @DisplayName("should use default topK of 5")
        void search_noTopKSpecified_usesDefaultTopK() {
            when(hybridSearchService.hybridSearch("test query", 5))
                    .thenReturn(Collections.emptyList());

            knowledgeBaseService.search("test query");

            verify(hybridSearchService).hybridSearch("test query", 5);
        }

        @Test
        @DisplayName("should use specified topK")
        void search_withTopK_usesSpecifiedTopK() {
            when(hybridSearchService.hybridSearch("test query", 10))
                    .thenReturn(Collections.emptyList());

            knowledgeBaseService.search("test query", 10);

            verify(hybridSearchService).hybridSearch("test query", 10);
        }
    }

    @Nested
    @DisplayName("clearAll()")
    class ClearAllTests {

        @Test
        @DisplayName("should clear all documents")
        void clearAll_executes() {
            assertDoesNotThrow(() -> knowledgeBaseService.clearAll());
        }
    }

    @Nested
    @DisplayName("getDocumentCount()")
    class GetDocumentCountTests {

        @Test
        @DisplayName("should return document count")
        void getDocumentCount_returnsCount() {
            long count = knowledgeBaseService.getDocumentCount();

            assertEquals(0L, count);
        }
    }
}
