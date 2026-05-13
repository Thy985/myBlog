package com.xingchen.backend.service;

import com.xingchen.backend.service.impl.ImageServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ImageServiceImpl Tests")
class ImageServiceImplTest {

    private ImageServiceImpl imageService = new ImageServiceImpl();

    @Nested
    @DisplayName("searchCoverImage()")
    class SearchCoverImageTests {

        @Test
        @DisplayName("should return default image when service is disabled")
        void searchCoverImage_serviceDisabled_returnsDefault() {
            ReflectionTestUtils.setField(imageService, "serviceEnabled", false);

            String result = imageService.searchCoverImage("test keyword", 800, 600);

            assertTrue(result.contains("placeholder.com"));
        }

        @Test
        @DisplayName("should use Lorem Picsum when Unsplash key is not configured")
        void searchCoverImage_noUnsplashKey_usesPicsum() {
            ReflectionTestUtils.setField(imageService, "serviceEnabled", true);
            ReflectionTestUtils.setField(imageService, "unsplashAccessKey", "");

            String result = imageService.searchCoverImage("nature", 800, 600);

            assertTrue(result.contains("picsum.photos"));
        }

        @Test
        @DisplayName("should generate URL with keyword as seed")
        void searchCoverImage_withKeyword_generatesCorrectUrl() {
            ReflectionTestUtils.setField(imageService, "serviceEnabled", true);
            ReflectionTestUtils.setField(imageService, "unsplashAccessKey", "");

            String result = imageService.searchCoverImage("mountain", 800, 600);

            assertTrue(result.contains("picsum.photos"));
            assertTrue(result.contains("800"));
            assertTrue(result.contains("600"));
        }

        @Test
        @DisplayName("should return default image on exception")
        void searchCoverImage_exception_returnsDefault() {
            ReflectionTestUtils.setField(imageService, "serviceEnabled", true);
            ReflectionTestUtils.setField(imageService, "unsplashAccessKey", null);

            String result = imageService.searchCoverImage("test", 800, 600);

            assertTrue(result.contains("placeholder.com") || result.contains("picsum.photos"));
        }
    }

    @Nested
    @DisplayName("getRandomImage()")
    class GetRandomImageTests {

        @Test
        @DisplayName("should return default image when service is disabled")
        void getRandomImage_serviceDisabled_returnsDefault() {
            ReflectionTestUtils.setField(imageService, "serviceEnabled", false);

            String result = imageService.getRandomImage(800, 600);

            assertTrue(result.contains("placeholder.com"));
        }

        @Test
        @DisplayName("should return Picsum URL with random seed")
        void getRandomImage_enabled_returnsPicsumUrl() {
            ReflectionTestUtils.setField(imageService, "serviceEnabled", true);

            String result = imageService.getRandomImage(800, 600);

            assertTrue(result.contains("picsum.photos"));
            assertTrue(result.contains("800"));
            assertTrue(result.contains("600"));
        }

        @Test
        @DisplayName("should handle exception gracefully")
        void getRandomImage_exception_returnsDefault() {
            ReflectionTestUtils.setField(imageService, "serviceEnabled", true);

            String result = imageService.getRandomImage(800, 600);

            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("generateCoverPrompt()")
    class GenerateCoverPromptTests {

        @Test
        @DisplayName("should generate prompt with title")
        void generateCoverPrompt_withTitle_generatesCorrectPrompt() {
            String prompt = imageService.generateCoverPrompt("My Article Title", null);

            assertTrue(prompt.contains("My Article Title"));
            assertTrue(prompt.contains("modern flat design"));
            assertTrue(prompt.contains("blog cover"));
        }

        @Test
        @DisplayName("should generate prompt with topic when title is null")
        void generateCoverPrompt_withTopic_usesTopic() {
            String prompt = imageService.generateCoverPrompt("Title", "Technology");

            assertTrue(prompt.contains("Technology"));
        }

        @Test
        @DisplayName("should generate prompt with both title and topic")
        void generateCoverPrompt_withBoth_includesBoth() {
            String prompt = imageService.generateCoverPrompt("Java Tutorial", "Programming");

            assertTrue(prompt.contains("Java Tutorial") || prompt.contains("Programming"));
            assertTrue(prompt.contains("Cover image"));
        }

        @Test
        @DisplayName("should always include design style keywords")
        void generateCoverPrompt_alwaysIncludesStyle() {
            String prompt = imageService.generateCoverPrompt("Any Title", null);

            assertTrue(prompt.contains("modern flat design"));
            assertTrue(prompt.contains("clean background"));
            assertTrue(prompt.contains("high quality"));
        }
    }

    @Nested
    @DisplayName("isAvailable()")
    class IsAvailableTests {

        @Test
        @DisplayName("should return true when service is enabled")
        void isAvailable_enabled_returnsTrue() {
            ReflectionTestUtils.setField(imageService, "serviceEnabled", true);

            assertTrue(imageService.isAvailable());
        }

        @Test
        @DisplayName("should return false when service is disabled")
        void isAvailable_disabled_returnsFalse() {
            ReflectionTestUtils.setField(imageService, "serviceEnabled", false);

            assertFalse(imageService.isAvailable());
        }
    }

    @Nested
    @DisplayName("getFallbackImage()")
    class GetFallbackImageTests {

        @Test
        @DisplayName("should return fallback image URL")
        void getFallbackImage_returnsValidUrl() {
            String result = imageService.getFallbackImage("technology", 800, 600);

            assertNotNull(result);
            assertTrue(result.contains("800") || result.contains("picsum") || result.contains("placeholder"));
        }

        @Test
        @DisplayName("should handle null topic")
        void getFallbackImage_nullTopic_handlesGracefully() {
            String result = imageService.getFallbackImage(null, 800, 600);

            assertNotNull(result);
        }
    }
}
