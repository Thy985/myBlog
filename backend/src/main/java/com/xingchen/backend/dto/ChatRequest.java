package com.xingchen.backend.dto;

import lombok.Data;

@Data
public class ChatRequest {
    private String message;
    private String sessionId;
    private Boolean useRag;
}
