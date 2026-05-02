package com.xingchen.backend.agent.react;

import com.xingchen.backend.ai.tool.Tool;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class ReActRequest {

    private Long userId;

    private String sessionId;

    private String message;

    private List<Map<String, String>> history;

    private List<Tool> tools;

    @Builder.Default
    private boolean useMemory = true;

    @Builder.Default
    private boolean useRag = true;

    @Builder.Default
    private int maxIterations = 10;
}
