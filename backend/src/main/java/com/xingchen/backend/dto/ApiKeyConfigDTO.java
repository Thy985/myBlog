package com.xingchen.backend.dto;

import lombok.Data;

@Data
public class ApiKeyConfigDTO {
    private String provider;
    private String apiKey;
    private String baseUrl;
    private String defaultModel;
    private Integer quota;
    private Double temperature;
    private Integer maxTokens;
    private Double topP;
}
