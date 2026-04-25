package com.xingchen.backend.ai.tool;

import cn.dev33.satoken.stp.StpUtil;
import com.xingchen.backend.common.PageResult;
import com.xingchen.backend.service.ArticleService;
import com.xingchen.backend.service.TavilySearchService;
import com.xingchen.backend.vo.ArticleListVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ContentOpportunityTool implements Tool {

    private final ArticleService articleService;
    private final TavilySearchService tavilySearchService;

    private static final List<String> POPULAR_TECH_TOPICS = List.of(
            "Docker", "Kubernetes", "微服务", "Spring Cloud", "Redis",
            "MySQL", "PostgreSQL", "MongoDB", "Elasticsearch",
            "Vue.js", "React", "Angular", "TypeScript", "Node.js",
            "Python", "Java", "Go", "Rust", "Kotlin",
            "机器学习", "深度学习", "TensorFlow", "PyTorch",
            "Docker Compose", "CI/CD", "Jenkins", "GitHub Actions",
            "AWS", "Azure", "阿里云", "Terraform",
            "REST API", "GraphQL", "gRPC", "WebSocket",
            "Kafka", "RabbitMQ", "Redis 缓存", "分布式锁",
            "Docker 部署", "K8s 部署", "云原生", "DevOps",
            "Git", "GitLab", "Maven", "Gradle",
            "单元测试", "集成测试", "E2E 测试", "测试驱动开发",
            "SOLID原则", "设计模式", "重构", "代码整洁",
            "Linux", "Nginx", "Apache", "Tomcat",
            "OAuth2", "JWT", "Spring Security", "网络安全",
            "MongoDB 索引", "SQL 优化", "数据库索引", "慢查询优化",
            "多线程", "并发编程", "异步编程", "响应式编程",
            "JVM 调优", "内存管理", "垃圾回收", "性能优化"
    );

    @Override
    public String getName() {
        return "content_opportunity";
    }

    @Override
    public String getDescription() {
        return "内容机会发现工具：分析用户搜索行为，发现内容缺口，推荐值得撰写的主题。";
    }

    @Override
    public ToolParameter[] getParameters() {
        return new ToolParameter[] {
            new ToolParameter(
                    "action",
                    "操作类型：discover（发现机会）| analyze_gaps（分析缺口）| recommend_topics（推荐主题）",
                    "string",
                    false,
                    "discover"
            ),
            new ToolParameter(
                    "category",
                    "关注的技术分类（如：backend, frontend, devops）",
                    "string",
                    false,
                    null
            ),
            new ToolParameter(
                    "limit",
                    "返回结果数量限制",
                    "integer",
                    false,
                    10
            ),
            new ToolParameter(
                    "searchOnline",
                    "是否联网搜索最新内容（true=使用Tavily API）",
                    "boolean",
                    false,
                    true
            )
        };
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters) {
        String action = (String) parameters.getOrDefault("action", "discover");
        String category = (String) parameters.get("category");
        Integer limit = parameters.get("limit") != null
                ? ((Number) parameters.get("limit")).intValue()
                : 10;
        Boolean searchOnline = parameters.get("searchOnline") != null
                ? (Boolean) parameters.get("searchOnline")
                : true;

        if (!StpUtil.isLogin()) {
            return ToolResult.error("用户未登录，无法分析内容机会");
        }
        Long userId = StpUtil.getLoginIdAsLong();

        return switch (action.toLowerCase()) {
            case "analyze_gaps" -> analyzeContentGaps(userId, category, limit, searchOnline);
            case "recommend_topics" -> recommendTopics(userId, category, limit, searchOnline);
            default -> discoverOpportunities(userId, category, limit, searchOnline);
        };
    }

    private ToolResult discoverOpportunities(Long userId, String category, int limit, boolean searchOnline) {
        log.info("发现用户 {} 的内容机会, searchOnline={}", userId, searchOnline);
        long startTime = System.currentTimeMillis();

        PageResult<ArticleListVO> pageResult = articleService.getUserArticles(userId, 1, 100);
        List<ArticleListVO> existingArticles = pageResult.getList();

        Set<String> existingTopics = extractTopics(existingArticles);
        List<Map<String, Object>> opportunities;

        if (searchOnline) {
            opportunities = discoverOnlineOpportunities(existingTopics, category, limit);
        } else {
            List<String> gaps = findContentGaps(existingTopics, category);
            opportunities = generateOpportunities(gaps, existingArticles);
        }

        long elapsed = System.currentTimeMillis() - startTime;
        Map<String, Object> result = new HashMap<>();
        result.put("totalOpportunities", opportunities.size());
        result.put("opportunities", opportunities.stream().limit(limit).collect(Collectors.toList()));
        result.put("existingTopics", existingTopics);
        result.put("searchSource", searchOnline ? "Tavily API" : "Local Analysis");
        result.put("analysisTime", elapsed);

        String message = searchOnline
                ? String.format("联网搜索完成！发现 %d 个热门内容机会（基于 Tavily 实时搜索）。分析耗时 %dms", opportunities.size(), elapsed)
                : String.format("本地分析完成！发现 %d 个潜在主题。分析耗时 %dms", opportunities.size(), elapsed);

        return ToolResult.success(result, message);
    }

    private List<Map<String, Object>> discoverOnlineOpportunities(Set<String> existingTopics, String category, int limit) {
        List<Map<String, Object>> opportunities = new ArrayList<>();

        String[] techDomains = category != null && !category.isEmpty()
                ? new String[]{category}
                : new String[]{"programming", "software development", "AI machine learning", "cloud computing", "DevOps"};

        Set<String> searchedTopics = new HashSet<>();

        for (String domain : techDomains) {
            try {
                TavilySearchService.TavilySearchResponse response = tavilySearchService.search(
                        "trending " + domain + " topics 2024 2025",
                        10
                );

                for (TavilySearchService.TavilySearchResult searchResult : response.getResults()) {
                    String topic = searchResult.getTitle();
                    if (topic == null || topic.isEmpty()) continue;

                    topic = cleanTopic(topic);
                    if (topic.length() < 3 || searchedTopics.contains(topic.toLowerCase())) continue;

                    boolean isGap = !existingTopics.contains(topic.toLowerCase());
                    searchedTopics.add(topic.toLowerCase());

                    Map<String, Object> opp = new HashMap<>();
                    opp.put("topic", topic);
                    opp.put("searchSource", "Tavily");
                    opp.put("trendingScore", searchResult.getScore());
                    opp.put("sourceUrl", searchResult.getUrl());
                    opp.put("sourceSnippet", searchResult.getSnippet());
                    opp.put("isGap", isGap);
                    opp.put("reason", isGap
                            ? "这是热门话题但您的博客尚未涉及"
                            : "您的博客已有相关主题，可进一步深化");
                    opp.put("articleTitle", generateArticleTitle(topic));
                    opp.put("keywords", getKeywords(topic));

                    opportunities.add(opp);

                    if (opportunities.size() >= limit * 2) break;
                }
            } catch (Exception e) {
                log.error("Tavily 搜索失败: domain={}", domain, e);
            }

            if (opportunities.size() >= limit * 2) break;
        }

        opportunities.sort((a, b) -> {
            double scoreA = (double) a.getOrDefault("trendingScore", 0.0);
            double scoreB = (double) b.getOrDefault("trendingScore", 0.0);
            return Double.compare(scoreB, scoreA);
        });

        return opportunities;
    }

    private String cleanTopic(String title) {
        if (title == null) return "";
        title = title.replaceAll("( - |_|\\|).*", "")
                     .replaceAll("\\d{4}-\\d{2}-\\d{2}", "")
                     .replaceAll("\\[.*?\\]", "")
                     .trim();
        return title;
    }

    private ToolResult analyzeContentGaps(Long userId, String category, int limit, boolean searchOnline) {
        return discoverOpportunities(userId, category, limit, searchOnline);
    }

    private ToolResult recommendTopics(Long userId, String category, int limit, boolean searchOnline) {
        return discoverOpportunities(userId, category, limit, searchOnline);
    }

    private ToolResult discoverOfflineOpportunities(Long userId, String category, int limit) {
        return discoverOpportunities(userId, category, limit, false);
    }

    private Set<String> extractTopics(List<ArticleListVO> articles) {
        Set<String> topics = new HashSet<>();
        for (ArticleListVO article : articles) {
            String title = article.getTitle() != null ? article.getTitle().toLowerCase() : "";
            String tags = article.getTagNames() != null ? article.getTagNames().toString().toLowerCase() : "";

            for (String tech : POPULAR_TECH_TOPICS) {
                if (title.contains(tech.toLowerCase()) || tags.contains(tech.toLowerCase())) {
                    topics.add(tech.toLowerCase());
                }
            }
        }
        return topics;
    }

    private List<String> findContentGaps(Set<String> existingTopics, String category) {
        List<String> gaps = new ArrayList<>();
        List<String> filteredTopics = POPULAR_TECH_TOPICS;

        if (category != null && !category.isEmpty()) {
            filteredTopics = filterTopicsByCategory(category);
        }

        for (String topic : filteredTopics) {
            if (!existingTopics.contains(topic.toLowerCase())) {
                gaps.add(topic);
            }
        }

        gaps.sort((a, b) -> {
            int scoreA = getPopularityScore(a);
            int scoreB = getPopularityScore(b);
            return Integer.compare(scoreB, scoreA);
        });

        return gaps;
    }

    private List<String> filterTopicsByCategory(String category) {
        Map<String, List<String>> categoryTopics = Map.of(
                "backend", List.of("Java", "Spring Boot", "Spring Cloud", "MySQL", "Redis", "PostgreSQL",
                        "Docker", "Kubernetes", "微服务", "REST API", "GraphQL", "JVM 调优", "多线程",
                        "OAuth2", "JWT", "Spring Security", "SQL 优化", "MongoDB", "Kafka", "RabbitMQ"),
                "frontend", List.of("Vue.js", "React", "Angular", "TypeScript", "JavaScript", "CSS",
                        "Node.js", "Webpack", "Vite", "Tailwind CSS", "Next.js", "Nuxt.js"),
                "devops", List.of("Docker", "Kubernetes", "Jenkins", "GitHub Actions", "CI/CD", "Linux",
                        "Nginx", "AWS", "阿里云", "Terraform", "Ansible", "Prometheus", "Grafana"),
                "ai", List.of("机器学习", "深度学习", "TensorFlow", "PyTorch", "OpenAI", "GPT",
                        "LangChain", "向量数据库", "RAG", "Embedding", "大模型", "AI Agent")
        );

        String lowerCategory = category.toLowerCase();
        if (categoryTopics.containsKey(lowerCategory)) {
            return categoryTopics.get(lowerCategory);
        }

        return POPULAR_TECH_TOPICS;
    }

    private int getPopularityScore(String topic) {
        Map<String, Integer> popularityMap = Map.ofEntries(
                Map.entry("docker", 10),
                Map.entry("kubernetes", 9),
                Map.entry("微服务", 9),
                Map.entry("redis", 9),
                Map.entry("spring boot", 10),
                Map.entry("mysql", 8),
                Map.entry("vue.js", 8),
                Map.entry("react", 8),
                Map.entry("python", 9),
                Map.entry("java", 10),
                Map.entry("深度学习", 9),
                Map.entry("机器学习", 9),
                Map.entry("docker compose", 7),
                Map.entry("ci/cd", 8),
                Map.entry("rest api", 7),
                Map.entry("typescript", 8),
                Map.entry("devops", 8),
                Map.entry("linux", 7),
                Map.entry("nginx", 7),
                Map.entry("postgresql", 7),
                Map.entry("mongodb", 7),
                Map.entry("elasticsearch", 7),
                Map.entry("kafka", 7),
                Map.entry("rabbitmq", 6),
                Map.entry("oauth2", 6),
                Map.entry("jwt", 7),
                Map.entry("graphql", 7),
                Map.entry("grpc", 6)
        );

        return popularityMap.getOrDefault(topic.toLowerCase(), 5);
    }

    private List<Map<String, Object>> generateOpportunities(List<String> gaps, List<ArticleListVO> existingArticles) {
        List<Map<String, Object>> opportunities = new ArrayList<>();

        for (int i = 0; i < gaps.size(); i++) {
            String topic = gaps.get(i);
            int priority = i + 1;
            int popularity = getPopularityScore(topic);

            Map<String, Object> opp = new HashMap<>();
            opp.put("topic", topic);
            opp.put("priority", priority);
            opp.put("popularityScore", popularity);
            opp.put("difficulty", getDifficulty(topic));
            opp.put("estimatedTime", getEstimatedTime(topic));
            opp.put("articleTitle", generateArticleTitle(topic));
            opp.put("keywords", getKeywords(topic));
            opp.put("reason", generateReason(topic));

            opportunities.add(opp);
        }

        return opportunities;
    }

    private String getDifficulty(String topic) {
        Set<String> advanced = Set.of("kubernetes", "spring cloud", "分布式锁", "jvm 调优", "深度学习", "k8s 部署");
        Set<String> intermediate = Set.of("docker", "redis", "微服务", "graphql", "grpc", "机器学习", "ci/cd");

        String lower = topic.toLowerCase();
        if (advanced.contains(lower)) return "高级";
        if (intermediate.contains(lower)) return "中级";
        return "入门";
    }

    private String getEstimatedTime(String topic) {
        Set<String> quick = Set.of("docker 部署", "rest api", "jwt", "oauth2", "单元测试");
        Set<String> medium = Set.of("docker", "redis", "vue.js", "react", "mysql", "nginx");

        String lower = topic.toLowerCase();
        if (quick.contains(lower)) return "1-2小时";
        if (medium.contains(lower)) return "2-4小时";
        return "4-8小时";
    }

    private String generateArticleTitle(String topic) {
        List<String> templates = List.of(
                "%s 完整指南",
                "深入理解 %s",
                "%s 快速入门",
                " %s 实战教程",
                " %s 原理与实践",
                "%s 最佳实践"
        );
        int index = Math.abs(topic.hashCode()) % templates.size();
        return String.format(templates.get(index), topic);
    }

    private List<String> getKeywords(String topic) {
        Map<String, List<String>> topicKeywords = Map.ofEntries(
                Map.entry("docker", List.of("Docker", "容器化", "部署", "镜像", "容器")),
                Map.entry("kubernetes", List.of("K8s", "容器编排", "集群", "Pod", "Service")),
                Map.entry("redis", List.of("Redis", "缓存", "NoSQL", "数据结构", "持久化")),
                Map.entry("spring boot", List.of("Spring Boot", "Spring", "Java", "框架", "快速开发")),
                Map.entry("vue.js", List.of("Vue", "前端", "响应式", "组件化", "Vue3")),
                Map.entry("react", List.of("React", "前端", "Hooks", "虚拟DOM", "组件化")),
                Map.entry("mysql", List.of("MySQL", "数据库", "SQL", "索引", "事务")),
                Map.entry("深度学习", List.of("深度学习", "神经网络", "TensorFlow", "PyTorch", "AI")),
                Map.entry("机器学习", List.of("机器学习", "算法", "监督学习", "无监督学习", "模型训练")),
                Map.entry("微服务", List.of("微服务", "分布式", "服务拆分", "Spring Cloud", "架构"))
        );

        String lower = topic.toLowerCase();
        return topicKeywords.getOrDefault(lower, List.of(topic, "教程", "指南", "实战"));
    }

    private String generateReason(String topic) {
        return String.format(" '%s' 是技术博客的高搜索量主题，目前您的博客还没有相关深入内容。撰写这类文章可以吸引搜索流量，提升博客价值。", topic);
    }

    @Override
    public long getTimeout() {
        return 30000;
    }
}
