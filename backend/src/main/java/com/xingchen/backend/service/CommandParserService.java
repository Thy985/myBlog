package com.xingchen.backend.service;

import com.xingchen.backend.dto.CommandDTO;

public interface CommandParserService {
    
    CommandDTO parse(String userMessage);
    
    String classifyIntent(String userMessage);
}
