package com.xingchen.backend.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xingchen.backend.dto.CommandDTO;
import com.xingchen.backend.service.CommandParserService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 命令解析服务（LLM 优先 + 正则兜底）
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CommandParserServiceImpl implements CommandParserService {

    private final ChatLanguageModel chatLanguageModel;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String INTENT_PROMPT = """
            你是意图分类器。根据用户消息输出 JSON：{"intent":"xxx","topic":"yyy"}
            
            intent 可选值：
            - create_article：明确要求写/生成/创作文章
            - schedule_task：创建定时任务（含"每天""每周""定时"等时间词+写文章）
            - list_tasks：查看任务列表
            - cancel_task：取消/删除任务
            - publish_article：发布文章（含"发布"+文章ID/编号）
            - edit_article：编辑/修改文章内容（含"编辑""修改"+文章ID/编号）
            - create_category：创建文章分类（含"创建分类""新建分类""添加分类"）
            - create_tag：创建标签（含"创建标签""新建标签""添加标签"）
            - chat：普通对话
            
            注意："写文章技巧""怎么写文章"是 chat，不是 create_article。
            只输出 JSON，不要其他内容。
            """;

    @Override
    public CommandDTO parse(String userMessage) {
        String intent = classifyIntent(userMessage);
        return switch (intent) {
            case "create_article" -> parseCreateArticle(userMessage);
            case "schedule_task" -> parseScheduleTask(userMessage);
            case "cancel_task" -> parseCancelTask(userMessage);
            case "list_tasks" -> CommandDTO.listTasks();
            case "publish_article" -> parsePublish(userMessage);
            case "edit_article" -> parseEditArticle(userMessage);
            case "create_category" -> parseCreateCategory(userMessage);
            case "create_tag" -> parseCreateTag(userMessage);
            default -> CommandDTO.chat(userMessage);
        };
    }

    @Override
    public String classifyIntent(String userMessage) {
        // LLM 优先
        try {
            List<ChatMessage> msgs = List.of(
                    SystemMessage.from(INTENT_PROMPT),
                    UserMessage.from(userMessage)
            );
            Response<AiMessage> resp = chatLanguageModel.generate(msgs);
            String raw = resp.content().text().trim();
            JsonNode json = objectMapper.readTree(extractJson(raw));
            String intent = json.get("intent").asText("chat");
            log.debug("LLM 意图: '{}' -> {}", userMessage, intent);
            return intent;
        } catch (Exception e) {
            log.warn("LLM 意图分类失败，降级规则: {}", e.getMessage());
        }

        // 规则兜底
        return classifyByRules(userMessage);
    }

    private String classifyByRules(String msg) {
        String lower = msg.toLowerCase();
        if (containsAny(lower, "每天", "每周", "定时", "自动") && containsAny(lower, "写", "生成", "创作"))
            return "schedule_task";
        if (containsAny(lower, "取消任务", "删除任务", "停止任务"))
            return "cancel_task";
        if (containsAny(lower, "查看任务", "任务列表", "我的任务"))
            return "list_tasks";
        // 注意顺序："发布一篇"是生成文章，"发布文章123"是发布已有文章
        if (lower.matches(".*(?:发布|写|生成|创作)一篇.+")
                && !containsAny(lower, "怎么写", "如何写", "技巧", "方法", "建议"))
            return "create_article";
        if (containsAny(lower, "发布文章", "发布编号", "发布id") && lower.matches(".*\\d+.*"))
            return "publish_article";
        if (containsAny(lower, "编辑文章", "修改文章", "编辑编号", "修改编号", "编辑id", "修改id"))
            return "edit_article";
        if (containsAny(lower, "创建分类", "新建分类", "添加分类", "新建一个分类", "创建一个新分类"))
            return "create_category";
        if (containsAny(lower, "创建标签", "新建标签", "添加标签", "新建一个标签", "创建一个新标签"))
            return "create_tag";
        if (lower.matches(".*(?:帮我写|写一篇|生成一篇|创作一篇).+")
                && !containsAny(lower, "怎么写", "如何写", "技巧", "方法", "建议"))
            return "create_article";
        return "chat";
    }

    private CommandDTO parseCreateArticle(String msg) {
        String topic = msg.replaceAll("(?:帮我|请)?(?:写|生成|创作)(?:一篇)?", "")
                .replaceAll("(?:的)?(?:文章|博文|内容)$", "")
                .replaceAll("(?:关于|有关)", "").trim();
        if (topic.isEmpty()) topic = msg;

        Integer wordCount = null;
        Matcher m = Pattern.compile("(\\d+)\\s*[字词]").matcher(msg);
        if (m.find()) wordCount = Integer.parseInt(m.group(1));

        String style = null;
        if (msg.contains("技术")) style = "technical";
        else if (msg.contains("轻松") || msg.contains("有趣")) style = "casual";
        else if (msg.contains("专业")) style = "professional";

        return CommandDTO.createArticle(topic, wordCount, style);
    }

    private CommandDTO parseScheduleTask(String msg) {
        String topic = msg.replaceAll("(?:每天|每周|定时|自动).*?(?:帮我)?(?:写|生成|创作|发布)", "")
                .replaceAll("(?:一篇)?(?:关于)?", "")
                .replaceAll("(?:的)?(?:文章|博文)$", "").trim();

        int hour = 8;
        Matcher hm = Pattern.compile("(\\d{1,2})\\s*[点时:：]").matcher(msg);
        if (hm.find()) {
            hour = Integer.parseInt(hm.group(1));
            if (hour < 0 || hour > 23) hour = 8;
        }
        if (msg.contains("下午") && hour < 12) hour += 12;
        if (msg.contains("晚上") && hour < 18) hour = 20;

        String cron = String.format("0 0 %d * * ?", hour);
        return CommandDTO.scheduleTask(topic.isEmpty() ? "技术文章" : topic, cron);
    }

    private CommandDTO parseCancelTask(String msg) {
        Matcher m = Pattern.compile("(\\d+)").matcher(msg);
        if (m.find()) return CommandDTO.cancelTask(Long.parseLong(m.group(1)));
        return CommandDTO.chat(msg);
    }

    private CommandDTO parsePublish(String msg) {
        Matcher m = Pattern.compile("(\\d+)").matcher(msg);
        if (m.find()) return CommandDTO.publishArticle(Long.parseLong(m.group(1)));
        return CommandDTO.chat(msg);
    }

    private CommandDTO parseEditArticle(String msg) {
        // 提取文章ID
        Matcher idMatcher = Pattern.compile("(?:文章|编号|ID|id)?\\s*(\\d+)").matcher(msg);
        Long articleId = null;
        if (idMatcher.find()) {
            articleId = Long.parseLong(idMatcher.group(1));
        }

        // 提取编辑内容（去掉命令部分）
        String content = msg.replaceAll("(?:编辑|修改)(?:文章|编号|ID|id)?\\s*\\d*", "")
                .replaceAll("(?:改成|改为|更新为|修改为)", "")
                .trim();

        if (articleId == null) {
            return CommandDTO.chat("请指定要编辑的文章ID，例如：编辑文章123，将标题改为xxx");
        }

        return CommandDTO.editArticle(articleId, content);
    }

    private CommandDTO parseCreateCategory(String msg) {
        // 提取分类名称
        String categoryName = msg.replaceAll("(?:创建|新建|添加)(?:一个)?(?:新)?(?:的)?分类", "")
                .replaceAll("(?:叫|名为|名称是)?", "")
                .trim();

        if (categoryName.isEmpty()) {
            return CommandDTO.chat("请指定分类名称，例如：创建一个分类叫技术分享");
        }

        return CommandDTO.createCategory(categoryName);
    }

    private CommandDTO parseCreateTag(String msg) {
        // 提取标签名称
        String tagName = msg.replaceAll("(?:创建|新建|添加)(?:一个)?(?:新)?(?:的)?标签", "")
                .replaceAll("(?:叫|名为|名称是)?", "")
                .trim();

        if (tagName.isEmpty()) {
            return CommandDTO.chat("请指定标签名称，例如：创建一个标签叫Java");
        }

        return CommandDTO.createTag(tagName);
    }

    private String extractJson(String text) {
        var m = Pattern.compile("\\{.*}", Pattern.DOTALL).matcher(text);
        return m.find() ? m.group() : text;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) if (text.contains(kw)) return true;
        return false;
    }
}
