package com.xingchen.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SEOService {

    private static final int TITLE_MAX_LENGTH = 60;
    private static final int DESCRIPTION_MAX_LENGTH = 160;
    private static final int KEYWORD_MIN_FREQ = 2;

    private static final List<String> SEO_STOP_WORDS = Arrays.asList(
            "的", "了", "和", "是", "在", "我", "有", "个", "人", "这",
            "不", "也", "就", "你", "都", "要", "会", "对", "与", "及",
            "为", "或", "等", "但", "而是", "而是", "因为", "所以", "如果"
    );

    private static final Pattern CHINESE_PATTERN = Pattern.compile("[\u4e00-\u9fa5]+");
    private static final Pattern WORD_PATTERN = Pattern.compile("[a-zA-Z0-9]+");

    public static class KeywordInfo {
        private final String keyword;
        private final int frequency;
        private final double score;

        public KeywordInfo(String keyword, int frequency, double score) {
            this.keyword = keyword;
            this.frequency = frequency;
            this.score = score;
        }

        public String getKeyword() { return keyword; }
        public int getFrequency() { return frequency; }
        public double getScore() { return score; }
    }

    public static class SEOAnalysisResult {
        private final List<KeywordInfo> keywords;
        private final String optimizedTitle;
        private final String metaDescription;
        private final List<String> internalLinks;
        private final Map<String, Object> seoScore;

        public SEOAnalysisResult(List<KeywordInfo> keywords, String optimizedTitle,
                                String metaDescription, List<String> internalLinks,
                                Map<String, Object> seoScore) {
            this.keywords = keywords;
            this.optimizedTitle = optimizedTitle;
            this.metaDescription = metaDescription;
            this.internalLinks = internalLinks;
            this.seoScore = seoScore;
        }

        public List<KeywordInfo> getKeywords() { return keywords; }
        public String getOptimizedTitle() { return optimizedTitle; }
        public String getMetaDescription() { return metaDescription; }
        public List<String> getInternalLinks() { return internalLinks; }
        public Map<String, Object> getSeoScore() { return seoScore; }
    }

    public List<KeywordInfo> extractKeywords(String content, int topK) {
        if (content == null || content.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, Integer> wordFreq = new HashMap<>();

        Matcher chineseMatcher = CHINESE_PATTERN.matcher(content);
        while (chineseMatcher.find()) {
            String word = chineseMatcher.group();
            if (word.length() >= 2 && !SEO_STOP_WORDS.contains(word)) {
                wordFreq.merge(word, 1, Integer::sum);
            }
        }

        Matcher wordMatcher = WORD_PATTERN.matcher(content);
        while (wordMatcher.find()) {
            String word = wordMatcher.group().toLowerCase();
            if (word.length() >= 3) {
                wordFreq.merge(word, 1, Integer::sum);
            }
        }

        List<KeywordInfo> keywords = wordFreq.entrySet().stream()
                .filter(e -> e.getValue() >= KEYWORD_MIN_FREQ)
                .map(e -> new KeywordInfo(e.getKey(), e.getValue(),
                        calculateKeywordScore(e.getKey(), e.getValue(), content.length())))
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .limit(topK)
                .collect(Collectors.toList());

        log.debug("提取关键词: contentLength={}, keywordCount={}", content.length(), keywords.size());
        return keywords;
    }

    private double calculateKeywordScore(String keyword, int frequency, int totalLength) {
        double freqScore = Math.min(frequency / 10.0, 1.0);
        double lengthScore = keyword.length() >= 4 ? 1.0 : 0.7;
        double normalizedFreq = (double) frequency / totalLength * 10000;
        double densityScore = Math.min(normalizedFreq, 1.0);

        return freqScore * 0.3 + lengthScore * 0.3 + densityScore * 0.4;
    }

    public String optimizeTitle(String title, String primaryKeyword) {
        if (title == null || title.isEmpty()) {
            return primaryKeyword != null ? primaryKeyword : "未命名";
        }

        String optimized = title.trim();

        if (primaryKeyword != null && !optimized.contains(primaryKeyword)) {
            if (optimized.length() + primaryKeyword.length() + 3 <= TITLE_MAX_LENGTH) {
                optimized = primaryKeyword + " - " + optimized;
            }
        }

        if (optimized.length() > TITLE_MAX_LENGTH) {
            optimized = optimized.substring(0, TITLE_MAX_LENGTH - 3) + "...";
        }

        return optimized;
    }

    public String generateMetaDescription(String content, String primaryKeyword, int maxLength) {
        if (content == null || content.isEmpty()) {
            return "";
        }

        String plainText = content
                .replaceAll("#+\\s*", "")
                .replaceAll("\\*+", "")
                .replaceAll("\\[.*?\\]\\(.*?\\)", "")
                .replaceAll("`.*?`", "")
                .replaceAll("\n+", " ")
                .replaceAll("\\s+", " ")
                .trim();

        String description = plainText;
        if (description.length() > maxLength) {
            int cutoff = maxLength;
            for (int i = maxLength; i > maxLength - 50 && i < description.length(); i++) {
                if (description.charAt(i) == ' ' || description.charAt(i) == '，') {
                    cutoff = i;
                    break;
                }
            }
            description = description.substring(0, cutoff);
            if (!description.endsWith("。") && !description.endsWith(",")) {
                description += "...";
            } else {
                description = description.substring(0, cutoff - 3) + "...";
            }
        }

        if (primaryKeyword != null && !description.contains(primaryKeyword)) {
            description = primaryKeyword + "：" + description;
            if (description.length() > maxLength) {
                description = description.substring(0, maxLength - 3) + "...";
            }
        }

        return description;
    }

    public SEOAnalysisResult analyzeSEO(String content, String title, String primaryKeyword) {
        List<KeywordInfo> keywords = extractKeywords(content, 10);

        String optimizedTitle = optimizeTitle(title, primaryKeyword);
        String metaDescription = generateMetaDescription(content, primaryKeyword, DESCRIPTION_MAX_LENGTH);

        List<String> internalLinks = extractInternalLinkCandidates(content);

        Map<String, Object> seoScore = calculateSEOScore(content, title, keywords, primaryKeyword);

        return new SEOAnalysisResult(keywords, optimizedTitle, metaDescription, internalLinks, seoScore);
    }

    private List<String> extractInternalLinkCandidates(String content) {
        Set<String> linkCandidates = new LinkedHashSet<>();
        Pattern linkPattern = Pattern.compile("\"([^\"]+)\"");
        Matcher matcher = linkPattern.matcher(content);
        while (matcher.find()) {
            String phrase = matcher.group(1);
            if (phrase.length() >= 4 && phrase.length() <= 20) {
                linkCandidates.add(phrase);
            }
        }
        return new ArrayList<>(linkCandidates).stream().limit(5).collect(Collectors.toList());
    }

    private Map<String, Object> calculateSEOScore(String content, String title,
                                                  List<KeywordInfo> keywords, String primaryKeyword) {
        Map<String, Object> score = new HashMap<>();

        int contentLength = content != null ? content.length() : 0;
        score.put("contentLength", contentLength);
        score.put("hasEnoughContent", contentLength >= 500);
        score.put("contentQuality", contentLength >= 1000 ? "good" : contentLength >= 500 ? "medium" : "low");

        boolean titleHasKeyword = title != null && primaryKeyword != null && title.contains(primaryKeyword);
        score.put("titleOptimization", titleHasKeyword ? "good" : "needs_improvement");

        boolean hasHeadings = content != null && content.matches("(?s).*#\\s+.*");
        score.put("hasHeadings", hasHeadings);

        boolean hasLists = content != null && content.matches("(?s).*[-*]\\s+.*");
        score.put("hasLists", hasLists);

        double keywordDensity = 0.0;
        if (primaryKeyword != null && content != null && !content.isEmpty()) {
            int count = content.split(primaryKeyword, -1).length - 1;
            keywordDensity = (double) count / content.length() * 100;
        }
        score.put("keywordDensity", String.format("%.2f%%", keywordDensity));
        score.put("keywordDensityGood", keywordDensity >= 0.5 && keywordDensity <= 3.0);

        int scoreValue = 0;
        if (contentLength >= 500) scoreValue += 20;
        if (contentLength >= 1000) scoreValue += 20;
        if (titleHasKeyword) scoreValue += 20;
        if (hasHeadings) scoreValue += 15;
        if (hasLists) scoreValue += 10;
        if (keywordDensity >= 0.5 && keywordDensity <= 3.0) scoreValue += 15;
        score.put("totalScore", scoreValue);
        score.put("grade", scoreValue >= 80 ? "A" : scoreValue >= 60 ? "B" : scoreValue >= 40 ? "C" : "D");

        return score;
    }

    public String buildSEOOptimizedPrompt(String topic, int wordCount, String style,
                                          String primaryKeyword, List<String> secondaryKeywords) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请撰写一篇关于\"").append(topic).append("\"的SEO优化博客文章。\n\n");

        prompt.append("SEO要求：\n");
        if (primaryKeyword != null) {
            prompt.append("1. 主关键词：").append(primaryKeyword).append("\n");
        }
        if (secondaryKeywords != null && !secondaryKeywords.isEmpty()) {
            prompt.append("2. 次关键词：").append(String.join("、", secondaryKeywords)).append("\n");
        }
        prompt.append("3. 关键词密度保持在0.5%-3%之间\n");
        prompt.append("4. 标题必须包含主关键词\n");
        prompt.append("5. 文章开头段落必须包含主关键词\n\n");

        String styleDesc = switch (style.toLowerCase()) {
            case "technical", "tech" -> "技术专业风格，使用专业术语，结构清晰";
            case "tutorial", "guide" -> "教程风格，步骤详细，易于理解";
            case "review" -> "评测风格，客观分析，优缺点并重";
            case "news" -> "新闻风格，简洁明了，事实为主";
            default -> "专业风格，结构清晰";
        };
        prompt.append("文章风格：").append(styleDesc).append("\n");
        prompt.append("目标字数：约").append(wordCount).append("字\n\n");
        prompt.append("格式要求：\n");
        prompt.append("1. 使用Markdown格式\n");
        prompt.append("2. 包含多级标题（H1/H2/H3）组织内容\n");
        prompt.append("3. 使用列表项增强可读性\n");
        prompt.append("4. 包含代码块或引用块（如适用）\n\n");
        prompt.append("请直接输出文章内容，不需要额外的说明。");

        return prompt.toString();
    }

    public String extractPrimaryKeyword(String topic) {
        if (topic == null || topic.isEmpty()) {
            return null;
        }

        String cleaned = topic.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9\\s]", "").trim();

        String[] words = cleaned.split("\\s+");
        for (String word : words) {
            if (word.length() >= 2 && word.length() <= 10 && !SEO_STOP_WORDS.contains(word)) {
                return word;
            }
        }

        return words.length > 0 ? words[0] : topic;
    }
}