package com.xingchen.backend.ab;

import com.xingchen.backend.cache.CacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.*;

/**
 * A/B 测试服务
 *
 * 支持模型/提示词/参数的 A/B 测试
 */
@Service
@Slf4j
public class ABTestService {

    private final CacheService cacheService;

    // 实验配置缓存
    private final Map<String, Experiment> experiments = new HashMap<>();

    public ABTestService(CacheService cacheService) {
        this.cacheService = cacheService;
        initExperiments();
    }

    private void initExperiments() {
        // 模型对比实验
        Experiment modelExp = new Experiment();
        modelExp.setId("model_comparison");
        modelExp.setName("模型效果对比");
        modelExp.setDescription("对比 glm-4-flash 和 glm-4-air 的效果");

        Variant control = new Variant();
        control.setId("control");
        control.setName("对照组");
        control.setConfig(Map.of("model", "glm-4-flash"));
        control.setTraffic(50);

        Variant treatment = new Variant();
        treatment.setId("treatment");
        treatment.setName("实验组");
        treatment.setConfig(Map.of("model", "glm-4-air"));
        treatment.setTraffic(50);

        modelExp.setVariants(Arrays.asList(control, treatment));
        experiments.put("model_comparison", modelExp);

        // 温度参数实验
        Experiment tempExp = new Experiment();
        tempExp.setId("temperature_test");
        tempExp.setName("温度参数测试");
        tempExp.setDescription("测试不同 temperature 对代码生成的影响");

        Variant low = new Variant();
        low.setId("low");
        low.setName("低温度");
        low.setConfig(Map.of("temperature", 0.3));
        low.setTraffic(33);

        Variant medium = new Variant();
        medium.setId("medium");
        medium.setName("中温度");
        medium.setConfig(Map.of("temperature", 0.7));
        medium.setTraffic(33);

        Variant high = new Variant();
        high.setId("high");
        high.setName("高温度");
        high.setConfig(Map.of("temperature", 0.9));
        high.setTraffic(34);

        tempExp.setVariants(Arrays.asList(low, medium, high));
        experiments.put("temperature_test", tempExp);

        // RAG 实验
        Experiment ragExp = new Experiment();
        ragExp.setId("rag_comparison");
        ragExp.setName("RAG 效果对比");
        ragExp.setDescription("对比有无知识库增强的效果");

        Variant noRag = new Variant();
        noRag.setId("no_rag");
        noRag.setName("无 RAG");
        noRag.setConfig(Map.of("useRag", false));
        noRag.setTraffic(50);

        Variant withRag = new Variant();
        withRag.setId("with_rag");
        withRag.setName("有 RAG");
        withRag.setConfig(Map.of("useRag", true));
        withRag.setTraffic(50);

        ragExp.setVariants(Arrays.asList(noRag, withRag));
        experiments.put("rag_comparison", ragExp);
    }

    /**
     * 获取用户分组
     */
    public Variant assignVariant(String experimentId, Long userId) {
        Experiment experiment = experiments.get(experimentId);
        if (experiment == null) {
            log.warn("实验不存在: {}", experimentId);
            return null;
        }

        // 检查缓存
        String cacheKey = getAssignmentKey(experimentId, userId);
        Optional<String> cached = cacheService.get(cacheKey, String.class);

        if (cached.isPresent()) {
            return findVariant(experiment, cached.get());
        }

        // 新分配
        Variant assigned = doAssignment(experiment, userId);
        cacheService.set(cacheKey, assigned.getId(), Duration.ofDays(7));

        // 记录分配
        recordAssignment(experimentId, assigned.getId());

        return assigned;
    }

    /**
     * 执行分配
     */
    private Variant doAssignment(Experiment experiment, Long userId) {
        List<Variant> variants = experiment.getVariants();

        // 使用哈希确保同一用户始终分配到同一组
        int hash = hashUserId(userId.toString() + experiment.getId());
        int bucket = Math.abs(hash) % 100;

        int cumulative = 0;
        for (Variant variant : variants) {
            cumulative += variant.getTraffic();
            if (bucket < cumulative) {
                return variant;
            }
        }

        // 默认返回第一个
        return variants.get(0);
    }

    /**
     * 记录实验指标
     */
    public void recordMetric(String experimentId, String variantId, String metricName, double value) {
        String key = String.format("ab:%s:%s:%s", experimentId, variantId, metricName);

        // 记录到 Redis
        cacheService.increment(key + ":count", 1);
        cacheService.increment(key + ":sum", (long) (value * 100)); // 放大100倍避免小数

        log.debug("记录 A/B 指标: experiment={}, variant={}, metric={}, value={}",
                experimentId, variantId, metricName, value);
    }

    /**
     * 获取实验统计
     */
    public ExperimentStats getStats(String experimentId) {
        Experiment experiment = experiments.get(experimentId);
        if (experiment == null) {
            return null;
        }

        Map<String, VariantStats> variantStats = new HashMap<>();

        for (Variant variant : experiment.getVariants()) {
            VariantStats stats = new VariantStats();
            stats.setVariantId(variant.getId());
            stats.setVariantName(variant.getName());

            // 获取指标
            stats.setAssignments(getMetricCount(experimentId, variant.getId(), "assignments"));
            stats.setLatencyAvg(getMetricAvg(experimentId, variant.getId(), "latency"));
            stats.setTokenAvg(getMetricAvg(experimentId, variant.getId(), "tokens"));
            stats.setSatisfactionRate(getMetricAvg(experimentId, variant.getId(), "satisfaction"));

            variantStats.put(variant.getId(), stats);
        }

        ExperimentStats result = new ExperimentStats();
        result.setExperimentId(experimentId);
        result.setExperimentName(experiment.getName());
        result.setVariantStats(variantStats);

        return result;
    }

    /**
     * 列出所有实验
     */
    public List<Experiment> listExperiments() {
        return new ArrayList<>(experiments.values());
    }

    /**
     * 创建新实验
     */
    public void createExperiment(Experiment experiment) {
        experiments.put(experiment.getId(), experiment);
        log.info("创建 A/B 实验: {}", experiment.getId());
    }

    private Variant findVariant(Experiment experiment, String variantId) {
        return experiment.getVariants().stream()
                .filter(v -> v.getId().equals(variantId))
                .findFirst()
                .orElse(null);
    }

    private void recordAssignment(String experimentId, String variantId) {
        recordMetric(experimentId, variantId, "assignments", 1);
    }

    private long getMetricCount(String experimentId, String variantId, String metric) {
        String key = String.format("ab:%s:%s:%s:count", experimentId, variantId, metric);
        Optional<Long> value = cacheService.get(key, Long.class);
        return value.orElse(0L);
    }

    private double getMetricAvg(String experimentId, String variantId, String metric) {
        String countKey = String.format("ab:%s:%s:%s:count", experimentId, variantId, metric);
        String sumKey = String.format("ab:%s:%s:%s:sum", experimentId, variantId, metric);

        Optional<Long> count = cacheService.get(countKey, Long.class);
        Optional<Long> sum = cacheService.get(sumKey, Long.class);

        if (count.isPresent() && count.get() > 0 && sum.isPresent()) {
            return sum.get() / 100.0 / count.get(); // 还原放大倍数
        }
        return 0;
    }

    private String getAssignmentKey(String experimentId, Long userId) {
        return String.format("ab:assignment:%s:%d", experimentId, userId);
    }

    private int hashUserId(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(input.getBytes());
            return (hash[0] & 0xFF) | ((hash[1] & 0xFF) << 8) |
                   ((hash[2] & 0xFF) << 16) | ((hash[3] & 0xFF) << 24);
        } catch (NoSuchAlgorithmException e) {
            return input.hashCode();
        }
    }

    // ========== 数据模型 ==========

    public static class Experiment {
        private String id;
        private String name;
        private String description;
        private List<Variant> variants;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public List<Variant> getVariants() { return variants; }
        public void setVariants(List<Variant> variants) { this.variants = variants; }
    }

    public static class Variant {
        private String id;
        private String name;
        private Map<String, Object> config;
        private int traffic; // 流量占比 (0-100)

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Map<String, Object> getConfig() { return config; }
        public void setConfig(Map<String, Object> config) { this.config = config; }
        public int getTraffic() { return traffic; }
        public void setTraffic(int traffic) { this.traffic = traffic; }
    }

    public static class ExperimentStats {
        private String experimentId;
        private String experimentName;
        private Map<String, VariantStats> variantStats;

        public String getExperimentId() { return experimentId; }
        public void setExperimentId(String experimentId) { this.experimentId = experimentId; }
        public String getExperimentName() { return experimentName; }
        public void setExperimentName(String experimentName) { this.experimentName = experimentName; }
        public Map<String, VariantStats> getVariantStats() { return variantStats; }
        public void setVariantStats(Map<String, VariantStats> variantStats) { this.variantStats = variantStats; }
    }

    public static class VariantStats {
        private String variantId;
        private String variantName;
        private long assignments;
        private double latencyAvg;
        private double tokenAvg;
        private double satisfactionRate;

        public String getVariantId() { return variantId; }
        public void setVariantId(String variantId) { this.variantId = variantId; }
        public String getVariantName() { return variantName; }
        public void setVariantName(String variantName) { this.variantName = variantName; }
        public long getAssignments() { return assignments; }
        public void setAssignments(long assignments) { this.assignments = assignments; }
        public double getLatencyAvg() { return latencyAvg; }
        public void setLatencyAvg(double latencyAvg) { this.latencyAvg = latencyAvg; }
        public double getTokenAvg() { return tokenAvg; }
        public void setTokenAvg(double tokenAvg) { this.tokenAvg = tokenAvg; }
        public double getSatisfactionRate() { return satisfactionRate; }
        public void setSatisfactionRate(double satisfactionRate) { this.satisfactionRate = satisfactionRate; }
    }
}
