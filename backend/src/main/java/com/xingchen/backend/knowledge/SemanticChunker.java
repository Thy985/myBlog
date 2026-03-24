package com.xingchen.backend.knowledge;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 语义化分块器
 * 基于语义边界进行智能分块
 */
@Component
@Slf4j
public class SemanticChunker {

    // 默认分块大小
    private static final int DEFAULT_CHUNK_SIZE = 800;
    // 重叠大小
    private static final int CHUNK_OVERLAP = 100;
    // 最小块大小
    private static final int MIN_CHUNK_SIZE = 200;

    // 句子结束符
    private static final Pattern SENTENCE_END = Pattern.compile("[。！？.!?]+");
    // 段落边界
    private static final Pattern PARAGRAPH_BOUNDARY = Pattern.compile("\n\s*\n");
    // 代码块标记
    private static final Pattern CODE_BLOCK = Pattern.compile("```[\\s\\S]*?```");
    // 标题标记
    private static final Pattern HEADING = Pattern.compile("^#{1,6}\\s+.+$", Pattern.MULTILINE);

    /**
     * 语义化分块
     */
    public List<Chunk> chunk(String content) {
        if (content == null || content.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<Chunk> chunks = new ArrayList<>();
        
        // 1. 先按代码块和文本分离
        List<Segment> segments = splitByCodeBlocks(content);
        
        for (Segment segment : segments) {
            if (segment.isCode()) {
                // 代码块作为一个整体
                chunks.add(createChunk(segment.content(), ChunkType.CODE, ""));
            } else {
                // 文本进行语义分块
                chunks.addAll(chunkText(segment.content()));
            }
        }
        
        // 2. 添加重叠上下文
        return addOverlap(chunks);
    }

    /**
     * 文本语义分块
     */
    private List<Chunk> chunkText(String text) {
        List<Chunk> chunks = new ArrayList<>();
        
        // 按段落分割
        String[] paragraphs = PARAGRAPH_BOUNDARY.split(text);
        
        StringBuilder currentChunk = new StringBuilder();
        String currentContext = "";
        
        for (String paragraph : paragraphs) {
            paragraph = paragraph.trim();
            if (paragraph.isEmpty()) continue;
            
            // 检测标题
            if (HEADING.matcher(paragraph).matches()) {
                // 标题作为上下文
                currentContext = paragraph.replaceAll("^#+\\s*", "").trim();
                
                // 如果当前块有内容，先保存
                if (currentChunk.length() > MIN_CHUNK_SIZE) {
                    chunks.add(createChunk(currentChunk.toString(), ChunkType.TEXT, currentContext));
                    currentChunk = new StringBuilder();
                }
                continue;
            }
            
            // 检查添加此段落后是否超出限制
            if (currentChunk.length() + paragraph.length() > DEFAULT_CHUNK_SIZE 
                    && currentChunk.length() > MIN_CHUNK_SIZE) {
                // 保存当前块
                chunks.add(createChunk(currentChunk.toString(), ChunkType.TEXT, currentContext));
                
                // 保留部分重叠
                String overlap = getOverlap(currentChunk.toString());
                currentChunk = new StringBuilder(overlap);
            }
            
            currentChunk.append(paragraph).append("\n\n");
        }
        
        // 保存最后一个块
        if (currentChunk.length() > 0) {
            chunks.add(createChunk(currentChunk.toString().trim(), ChunkType.TEXT, currentContext));
        }
        
        return chunks;
    }

    /**
     * 按代码块分割
     */
    private List<Segment> splitByCodeBlocks(String content) {
        List<Segment> segments = new ArrayList<>();
        Matcher matcher = CODE_BLOCK.matcher(content);
        
        int lastEnd = 0;
        while (matcher.find()) {
            // 代码块前的文本
            if (matcher.start() > lastEnd) {
                String text = content.substring(lastEnd, matcher.start());
                segments.add(new Segment(text, false));
            }
            // 代码块
            segments.add(new Segment(matcher.group(), true));
            lastEnd = matcher.end();
        }
        
        // 剩余文本
        if (lastEnd < content.length()) {
            segments.add(new Segment(content.substring(lastEnd), false));
        }
        
        return segments;
    }

    /**
     * 获取重叠部分（基于句子边界）
     */
    private String getOverlap(String content) {
        if (content.length() <= CHUNK_OVERLAP) {
            return content;
        }
        
        // 从后往前找句子边界
        String tail = content.substring(content.length() - CHUNK_OVERLAP);
        Matcher matcher = SENTENCE_END.matcher(tail);
        
        int lastSentenceEnd = 0;
        while (matcher.find()) {
            lastSentenceEnd = matcher.end();
        }
        
        if (lastSentenceEnd > 0) {
            return content.substring(content.length() - CHUNK_OVERLAP + lastSentenceEnd);
        }
        
        return tail;
    }

    /**
     * 添加块间重叠
     */
    private List<Chunk> addOverlap(List<Chunk> chunks) {
        if (chunks.size() <= 1) {
            return chunks;
        }
        
        List<Chunk> result = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            Chunk chunk = chunks.get(i);
            StringBuilder enhancedContent = new StringBuilder();
            
            // 添加上下文信息
            if (!chunk.context().isEmpty()) {
                enhancedContent.append("【上下文：").append(chunk.context()).append("】\n\n");
            }
            
            enhancedContent.append(chunk.content());
            
            // 添加前一个块的部分内容作为背景
            if (i > 0) {
                Chunk prevChunk = chunks.get(i - 1);
                String prevContext = getOverlap(prevChunk.content());
                if (!prevContext.isEmpty()) {
                    enhancedContent.insert(0, "【前文：" + prevContext + "】\n\n");
                }
            }
            
            result.add(new Chunk(
                enhancedContent.toString(),
                chunk.type(),
                chunk.context(),
                chunk.index(),
                chunk.metadata()
            ));
        }
        
        return result;
    }

    private Chunk createChunk(String content, ChunkType type, String context) {
        return new Chunk(content, type, context, 0, new java.util.HashMap<>());
    }

    // ========== 内部类 ==========
    
    private record Segment(String content, boolean isCode) {}
    
    public enum ChunkType {
        TEXT, CODE, HEADING, LIST
    }
    
    public record Chunk(
        String content,
        ChunkType type,
        String context,
        int index,
        java.util.Map<String, Object> metadata
    ) {}
}