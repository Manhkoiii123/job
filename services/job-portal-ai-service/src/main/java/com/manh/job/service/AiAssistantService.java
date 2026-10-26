package com.manh.job.service;

import com.manh.job.dto.ai.AiChatRequest;
import com.manh.job.dto.ai.AiChatResponse;

public interface AiAssistantService {
    AiChatResponse chat(AiChatRequest request);
}
